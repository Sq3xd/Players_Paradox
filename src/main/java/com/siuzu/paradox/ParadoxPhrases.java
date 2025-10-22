package com.siuzu.paradox;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParadoxPhrases {
    private static final Random RANDOM = new Random();

    private static final int HURT_COUNT = 102;
    private static final int REVENGE_COUNT = 103;

    private static final int GREETING_COUNT = 20;
    private static final int DIALOGUE_COUNT = 20;
    private static final int GIFT_COUNT = 15;
    private static final int GOODBYE_COUNT = 15;
    private static final int CONFUSED_COUNT = 10;

    private static final List<String> CONFUSED_KEYS = new ArrayList<>();
    private static final List<String> HURT_KEYS = new ArrayList<>();
    private static final List<String> REVENGE_KEYS = new ArrayList<>();
    private static final List<String> GREETING_KEYS = new ArrayList<>();
    private static final List<String> DIALOGUE_KEYS = new ArrayList<>();
    private static final List<String> GIFT_KEYS = new ArrayList<>();
    private static final List<String> GOODBYE_KEYS = new ArrayList<>();

    static {
        // Hurt
        for (int i = 1; i <= HURT_COUNT; i++) {
            HURT_KEYS.add("phrase.paradox.hurt_" + i);
        }

        // Revenge
        for (int i = 1; i <= REVENGE_COUNT; i++) {
            REVENGE_KEYS.add("phrase.paradox.revenge_" + i);
        }

        // Greeting
        for (int i = 1; i <= GREETING_COUNT; i++) {
            GREETING_KEYS.add("phrase.paradox.greeting_" + i);
        }

        // Dialogue
        for (int i = 1; i <= DIALOGUE_COUNT; i++) {
            DIALOGUE_KEYS.add("phrase.paradox.dialogue_" + i);
        }

        // Gift
        for (int i = 1; i <= GIFT_COUNT; i++) {
            GIFT_KEYS.add("phrase.paradox.gift_" + i);
        }

        // Goodbye
        for (int i = 1; i <= GOODBYE_COUNT; i++) {
            GOODBYE_KEYS.add("phrase.paradox.goodbye_" + i);
        }

        // Confused
        for (int i = 1; i <= CONFUSED_COUNT; i++) {
            CONFUSED_KEYS.add("phrase.paradox.confused_" + i);
        }
    }

    public static Component randomConfusedPhrase() {
        String key = CONFUSED_KEYS.get(RANDOM.nextInt(CONFUSED_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomHurtPhrase() {
        String key = HURT_KEYS.get(RANDOM.nextInt(HURT_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomRevengePhrase() {
        String key = REVENGE_KEYS.get(RANDOM.nextInt(REVENGE_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomGreetingPhrase() {
        String key = GREETING_KEYS.get(RANDOM.nextInt(GREETING_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomDialoguePhrase() {
        String key = DIALOGUE_KEYS.get(RANDOM.nextInt(DIALOGUE_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomGiftPhrase() {
        String key = GIFT_KEYS.get(RANDOM.nextInt(GIFT_KEYS.size()));
        return Component.translatable(key);
    }

    public static Component randomGoodbyePhrase() {
        String key = GOODBYE_KEYS.get(RANDOM.nextInt(GOODBYE_KEYS.size()));
        return Component.translatable(key);
    }
}