package me.ardelys.hac.checks;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.combat.*;
import me.ardelys.hac.checks.movement.*;
import me.ardelys.hac.checks.player.*;
import me.ardelys.hac.checks.world.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {

    private final HAC plugin;
    private final Map<Class<? extends Check>, Check> checksByClass = new ConcurrentHashMap<>();
    private final Map<String, Check> checksByName = new ConcurrentHashMap<>();

    public CheckManager(HAC plugin) {
        this.plugin = plugin;
        registerAllChecks();
    }

    private void registerAllChecks() {
        register(new KillAura(plugin));
        register(new AimAssist(plugin));
        register(new Reach(plugin));
        register(new AutoClicker(plugin));
        register(new Criticals(plugin));
        register(new VelocityCheck(plugin));
        register(new FastBow(plugin));
        register(new TriggerBot(plugin));

        register(new Speed(plugin));
        register(new Fly(plugin));
        register(new Glide(plugin));
        register(new Jesus(plugin));
        register(new Step(plugin));
        register(new HighJump(plugin));
        register(new LongJump(plugin));
        register(new NoFall(plugin));
        register(new NoSlow(plugin));
        register(new InventoryMove(plugin));
        register(new Phase(plugin));
        register(new Blink(plugin));

        register(new TimerCheck(plugin));
        register(new BadPackets(plugin));
        register(new FastEat(plugin));
        register(new FastProjectile(plugin));
        register(new ImpossibleActions(plugin));

        register(new FastPlace(plugin));
        register(new FastBreak(plugin));
        register(new Scaffold(plugin));
        register(new InvalidBlockInteraction(plugin));
    }

    private void register(Check check) {
        checksByClass.put(check.getClass(), check);
        checksByName.put(check.getName().toLowerCase(), check);
    }

    @SuppressWarnings("unchecked")
    public <T extends Check> T getCheck(Class<T> clazz) {
        return (T) checksByClass.get(clazz);
    }

    public Check getCheck(String name) {
        if (name == null) return null;
        return checksByName.get(name.toLowerCase());
    }

    public Collection<Check> getAllChecks() {
        return Collections.unmodifiableCollection(checksByClass.values());
    }

    public void reloadChecks() {
        for (Check check : checksByClass.values()) {
            check.reloadConfig();
        }
    }

    public void cleanupPlayer(UUID uuid) {
    }
}
