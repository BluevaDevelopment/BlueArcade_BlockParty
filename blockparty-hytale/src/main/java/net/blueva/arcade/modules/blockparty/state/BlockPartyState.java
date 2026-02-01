package net.blueva.arcade.modules.blockparty.state;

import net.blueva.arcade.api.world.BlockPattern;
import com.hypixel.hytale.math.vector.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockPartyState {
    private final int arenaId;
    private final FloorBounds floor;
    private final Map<String, BlockPattern<Location, String>> patterns;
    private final List<String> order;
    private final String initialPatternKey;
    private int round = 0;
    private double searchSeconds;
    private BlockPattern<Location, String> currentPattern;
    private Map<Location, String> currentBlocks = new HashMap<>();
    private String targetMaterial = "Empty";
    private RoundPhase phase = RoundPhase.MUSIC;
    private long phaseTicksRemaining = 0L;
    private long phaseTotalTicks = 0L;
    private double displayedTime = 0L;
    private String musicTrack;
    private boolean musicPaused;
    private boolean ended = false;

    public BlockPartyState(int arenaId, FloorBounds floor, Map<String, BlockPattern<Location, String>> patterns, List<String> order,
                           String initialPatternKey, double searchSeconds) {
        this.arenaId = arenaId;
        this.floor = floor;
        this.patterns = patterns;
        this.order = order;
        this.initialPatternKey = initialPatternKey;
        this.searchSeconds = searchSeconds;
    }

    public int getArenaId() {
        return arenaId;
    }

    public FloorBounds getFloor() {
        return floor;
    }

    public Map<String, BlockPattern<Location, String>> getPatterns() {
        return patterns;
    }

    public List<String> getOrder() {
        return order;
    }

    public String getInitialPatternKey() {
        return initialPatternKey;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public double getSearchSeconds() {
        return searchSeconds;
    }

    public void setSearchSeconds(double searchSeconds) {
        this.searchSeconds = searchSeconds;
    }

    public BlockPattern<Location, String> getCurrentPattern() {
        return currentPattern;
    }

    public void setCurrentPattern(BlockPattern<Location, String> currentPattern) {
        this.currentPattern = currentPattern;
    }

    public Map<Location, String> getCurrentBlocks() {
        return currentBlocks;
    }

    public void setCurrentBlocks(Map<Location, String> currentBlocks) {
        this.currentBlocks = currentBlocks;
    }

    public String getTargetMaterial() {
        return targetMaterial;
    }

    public void setTargetMaterial(String targetMaterial) {
        this.targetMaterial = targetMaterial;
    }

    public RoundPhase getPhase() {
        return phase;
    }

    public void setPhase(RoundPhase phase) {
        this.phase = phase;
    }

    public long getPhaseTicksRemaining() {
        return phaseTicksRemaining;
    }

    public void setPhaseTicksRemaining(long phaseTicksRemaining) {
        this.phaseTicksRemaining = phaseTicksRemaining;
    }

    public long getPhaseTotalTicks() {
        return phaseTotalTicks;
    }

    public void setPhaseTotalTicks(long phaseTotalTicks) {
        this.phaseTotalTicks = phaseTotalTicks;
    }

    public double getDisplayedTime() {
        return displayedTime;
    }

    public void setDisplayedTime(double displayedTime) {
        this.displayedTime = displayedTime;
    }

    public String getMusicTrack() {
        return musicTrack;
    }

    public void setMusicTrack(String musicTrack) {
        this.musicTrack = musicTrack;
    }

    public boolean isMusicPaused() {
        return musicPaused;
    }

    public void setMusicPaused(boolean musicPaused) {
        this.musicPaused = musicPaused;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }
}
