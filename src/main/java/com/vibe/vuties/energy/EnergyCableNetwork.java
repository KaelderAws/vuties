package com.vibe.vuties.energy;

import com.vibe.vuties.block.EnergyCableBlock;
import com.vibe.vuties.block.entity.EnergyCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.*;

public class EnergyCableNetwork {

    public static final int MAX_TRANSFER_RATE = 10000; // 10,000 FE/t throughput

    public record Endpoint(BlockPos cablePos, Direction direction, BlockPos neighborPos) {}
    public record ReceiverEntry(IEnergyStorage storage, BlockPos pos, int canAccept) {}
    public record SourceEntry(IEnergyStorage storage, BlockPos pos, int canGive) {}

    private final Set<BlockPos> cablePositions = new HashSet<>();
    private final List<Endpoint> endpoints = new ArrayList<>();
    private volatile boolean valid = true;
    private int conductingTicks = 0;
    private boolean isConducting = false;

    public EnergyCableNetwork(Collection<BlockPos> positions, Collection<Endpoint> endpoints) {
        this.cablePositions.addAll(positions);
        this.endpoints.addAll(endpoints);
    }

    public boolean isValid() {
        return valid;
    }

    public int getMemberCount() {
        return cablePositions.size();
    }

    public void invalidate() {
        this.valid = false;
    }

    public static EnergyCableNetwork buildNetwork(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<EnergyCableBlockEntity> memberList = new ArrayList<>();
        List<Endpoint> endpointList = new ArrayList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() <= 512) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) continue;

            BlockEntity be = level.getBlockEntity(current);
            if (!(be instanceof EnergyCableBlockEntity cableBE)) continue;

            BlockState state = level.getBlockState(current);
            if (!(state.getBlock() instanceof EnergyCableBlock)) continue;

            memberList.add(cableBE);

            for (Direction dir : Direction.values()) {
                if (EnergyCableBlock.isConnected(state, dir)) {
                    BlockPos neighborPos = current.relative(dir);
                    if (!level.isLoaded(neighborPos)) continue;

                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (neighborState.getBlock() instanceof EnergyCableBlock) {
                        if (visited.add(neighborPos)) {
                            queue.add(neighborPos);
                        }
                    } else {
                        endpointList.add(new Endpoint(current, dir, neighborPos));
                    }
                }
            }
        }

        EnergyCableNetwork network = new EnergyCableNetwork(visited, endpointList);
        EnergyCableBlockEntity master = null;
        for (EnergyCableBlockEntity member : memberList) {
            member.setNetwork(network);
            if (master == null || member.getBlockPos().compareTo(master.getBlockPos()) < 0) {
                master = member;
            }
        }

        for (EnergyCableBlockEntity member : memberList) {
            member.setMaster(member == master);
        }

        return network;
    }

    public void tick(Level level) {
        if (!valid || cablePositions.isEmpty()) return;

        if (conductingTicks > 0) {
            conductingTicks--;
            if (conductingTicks == 0) {
                updateConductingVisual(level, false);
            }
        }

        List<ReceiverEntry> receivers = new ArrayList<>();
        List<SourceEntry> sources = new ArrayList<>();

        for (Endpoint ep : endpoints) {
            if (!level.isLoaded(ep.neighborPos())) continue;
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, ep.neighborPos(), ep.direction().getOpposite());
            if (storage == null) continue;

            if (storage.canReceive()) {
                int canAccept = storage.receiveEnergy(MAX_TRANSFER_RATE, true);
                if (canAccept > 0) {
                    receivers.add(new ReceiverEntry(storage, ep.neighborPos(), canAccept));
                }
            }
            if (storage.canExtract()) {
                int canGive = storage.extractEnergy(MAX_TRANSFER_RATE, true);
                if (canGive > 0) {
                    sources.add(new SourceEntry(storage, ep.neighborPos(), canGive));
                }
            }
        }

        if (receivers.isEmpty() || sources.isEmpty()) return;

        int totalDemand = 0;
        for (ReceiverEntry r : receivers) {
            boolean hasDistinctSource = false;
            for (SourceEntry s : sources) {
                if (!s.pos().equals(r.pos())) {
                    hasDistinctSource = true;
                    break;
                }
            }
            if (hasDistinctSource) {
                totalDemand += r.canAccept();
            }
        }

        if (totalDemand <= 0) return;

        int toExtract = Math.min(totalDemand, MAX_TRANSFER_RATE);
        int extracted = 0;

        for (SourceEntry s : sources) {
            if (extracted >= toExtract) break;
            int maxPull = Math.min(toExtract - extracted, s.canGive());
            int pulled = s.storage().extractEnergy(maxPull, false);
            extracted += pulled;
        }

        if (extracted > 0) {
            int remaining = extracted;
            for (ReceiverEntry r : receivers) {
                if (remaining <= 0) break;
                int toSend = Math.min(remaining, r.canAccept());
                int accepted = r.storage().receiveEnergy(toSend, false);
                remaining -= accepted;
            }

            if (remaining > 0) {
                for (SourceEntry s : sources) {
                    if (remaining <= 0) break;
                    int returned = s.storage().receiveEnergy(remaining, false);
                    remaining -= returned;
                }
            }

            markConducting(level);
        }
    }

    public int insertEnergy(Level level, int maxReceive, boolean simulate, BlockPos fromPos) {
        if (!valid || maxReceive <= 0) return 0;

        int acceptedTotal = 0;
        int remaining = Math.min(maxReceive, MAX_TRANSFER_RATE);

        for (Endpoint ep : endpoints) {
            if (ep.neighborPos().equals(fromPos)) continue;
            if (!level.isLoaded(ep.neighborPos())) continue;

            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, ep.neighborPos(), ep.direction().getOpposite());
            if (storage != null && storage.canReceive()) {
                int accepted = storage.receiveEnergy(remaining, simulate);
                acceptedTotal += accepted;
                remaining -= accepted;
                if (remaining <= 0) break;
            }
        }

        if (!simulate && acceptedTotal > 0) {
            markConducting(level);
        }

        return acceptedTotal;
    }

    public void markConducting(Level level) {
        this.conductingTicks = 10;
        if (!this.isConducting) {
            updateConductingVisual(level, true);
        }
    }

    private void updateConductingVisual(Level level, boolean conducting) {
        this.isConducting = conducting;
        // Iterate over a snapshot list of positions to be 100% thread/concurrent-safe
        List<BlockPos> positions = new ArrayList<>(cablePositions);
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof EnergyCableBlock && state.getValue(EnergyCableBlock.CONDUCTING) != conducting) {
                // Use UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE (18) to avoid triggering neighbor updateShape calls
                level.setBlock(pos, state.setValue(EnergyCableBlock.CONDUCTING, conducting), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
