package org.schabi.newpipe.localserver;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    static class TopicCategory {
        String name;
        String icon;
        String[] topics;

        TopicCategory(String name, String icon, String[] topics) {
            this.name = name;
            this.icon = icon;
            this.topics = topics;
        }
    }

    private static final TopicCategory[] CATEGORIES = new TopicCategory[]{
        new TopicCategory("Gaming", "🎮", new String[]{
            "Gaming", "Minecraft", "Fortnite", "GTA", "Call of Duty",
            "Valorant", "League of Legends", "Pokemon", "Nintendo",
            "PlayStation", "Xbox", "PC Gaming", "Esports", "Speedruns",
            "Game Reviews", "Indie Games", "Retro Gaming", "Mobile Games",
            "Roblox", "Apex Legends", "FIFA"
        }),
        new TopicCategory("Music", "🎵", new String[]{
            "Music", "Pop Music", "Hip Hop", "R&B", "Rock", "Metal",
            "Jazz", "Classical", "Electronic", "EDM", "Lo-Fi", "K-Pop",
            "J-Pop", "Country", "Indie Music", "Music Production",
            "Guitar", "Piano", "Singing", "Music Theory", "Album Reviews",
            "Concerts", "DJ"
        }),
        new TopicCategory("Technology", "💻", new String[]{
            "Technology", "Programming", "Coding", "Web Development",
            "App Development", "AI", "Machine Learning", "Cybersecurity",
            "Linux", "Apple", "Android", "Smartphones", "Laptops",
            "PC Building", "Tech Reviews", "Gadgets", "Software",
            "Cloud Computing", "Blockchain", "Crypto", "Startups"
        }),
        new TopicCategory("Entertainment", "🎬", new String[]{
            "Movies", "TV Shows", "Netflix", "Anime", "Marvel", "DC",
            "Star Wars", "Disney", "Comedy", "Stand-up Comedy", "Drama",
            "Horror", "Sci-Fi", "Documentary", "Film Analysis",
            "Movie Reviews", "Behind the Scenes", "Celebrities",
            "Award Shows", "Trailers", "Fan Theories"
        }),
        new TopicCategory("Education", "📚", new String[]{
            "Science", "Physics", "Chemistry", "Biology", "Mathematics",
            "History", "Geography", "Psychology", "Philosophy",
            "Economics", "Finance", "Investing", "Business", "Marketing",
            "Language Learning", "English", "Spanish", "Study Tips",
            "College", "University", "Tutorials"
        }),
        new TopicCategory("Health & Fitness", "🏃", new String[]{
            "Fitness", "Workout", "Gym", "Yoga", "Running", "CrossFit",
            "Bodybuilding", "Weight Loss", "Nutrition", "Healthy Eating",
            "Mental Health", "Meditation", "Self Improvement",
            "Productivity", "Motivation", "Sports", "Basketball",
            "Football", "Soccer", "MMA", "Boxing", "Tennis", "Golf"
        }),
        new TopicCategory("Lifestyle", "🍳", new String[]{
            "Cooking", "Recipes", "Baking", "Food", "Restaurants",
            "Travel", "Vlogging", "Daily Vlog", "Fashion", "Style",
            "Beauty", "Skincare", "Home Decor", "Interior Design", "DIY",
            "Crafts", "Gardening", "Pets", "Dogs", "Cats", "Cars",
            "Motorcycles", "Photography"
        }),
        new TopicCategory("Creative", "🎨", new String[]{
            "Art", "Drawing", "Painting", "Digital Art", "Animation",
            "3D Modeling", "Graphic Design", "Video Editing", "Filmmaking",
            "Photography", "Music Production", "Writing", "Storytelling",
            "Architecture", "Fashion Design", "Crafts", "Woodworking",
            "Sculpture"
        }),
        new TopicCategory("Science & Nature", "🔬", new String[]{
            "Space", "Astronomy", "NASA", "Physics", "Nature", "Animals",
            "Wildlife", "Ocean", "Marine Life", "Environment", "Climate",
            "Geology", "Paleontology", "Dinosaurs", "Engineering",
            "Inventions", "Experiments"
        }),
        new TopicCategory("News & Current Events", "📰", new String[]{
            "News", "Politics", "World News", "Tech News", "Sports News",
            "Entertainment News", "Business News", "Analysis",
            "Commentary", "Podcasts", "Interviews", "Debates",
            "Current Events"
        })
    };

    private static final String[] BLOCKED_SUGGESTIONS = new String[]{
        "ASMR", "Unboxing", "Reaction", "Vlogs", "News", "Politics", "Gaming",
        "clickbait", "drama", "gossip", "challenge", "family vlog"
    };

    private HistoryDbHelper db;
    private final Set<String> preferredTopics = new HashSet<>();
    private final Set<String> blockedKeywords = new HashSet<>();
    private final Set<String> blockedChannels = new HashSet<>();

    private ChipGroup cgPreferredTopics;
    private TextView tvPreferredEmpty;
    private EditText etCustomInterest;
    private LinearLayout layoutCategoriesContainer;

    private EditText etCustomBlocked;
    private ChipGroup cgBlockedSuggestions;
    private ChipGroup cgBlockedKeywords;
    private TextView tvBlockedEmpty;

    private EditText etCustomBlockedChannel;
    private ChipGroup cgBlockedChannels;
    private TextView tvBlockedChannelsEmpty;

    private SwitchCompat swHideWatched;
    private SwitchCompat swHideShorts;

    // Track active category chips to toggle selection colors remotely
    private final Map<String, Chip> categoryChipsMap = new HashMap<>();
    private final Map<String, TextView> categoryCountViewsMap = new HashMap<>();

    private LinearLayout layoutBlockedCategoriesContainer;
    private final Map<String, Chip> blockedCategoryChipsMap = new HashMap<>();
    private final Map<String, TextView> blockedCategoryCountViewsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = HistoryDbHelper.getInstance(this);

        // Load data
        preferredTopics.addAll(db.getPreferredKeywords());
        blockedKeywords.addAll(db.getBlockedKeywords());
        blockedChannels.addAll(db.getBlockedChannels());

        // Init views
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Interests"));
        tabLayout.addTab(tabLayout.newTab().setText("Blocked"));

        LinearLayout layoutInterests = findViewById(R.id.layout_interests);
        LinearLayout layoutBlocked = findViewById(R.id.layout_blocked);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutInterests.setVisibility(View.VISIBLE);
                    layoutBlocked.setVisibility(View.GONE);
                } else {
                    layoutInterests.setVisibility(View.GONE);
                    layoutBlocked.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Interests views
        cgPreferredTopics = findViewById(R.id.cg_preferred_topics);
        tvPreferredEmpty = findViewById(R.id.tv_preferred_empty);
        etCustomInterest = findViewById(R.id.et_custom_interest);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        Button btnAddCustomInterest = findViewById(R.id.btn_add_custom_interest);

        btnAddCustomInterest.setOnClickListener(v -> addCustomPreferredTopic());

        // Blocked views
        etCustomBlocked = findViewById(R.id.et_custom_blocked);
        cgBlockedSuggestions = findViewById(R.id.cg_blocked_suggestions);
        cgBlockedKeywords = findViewById(R.id.cg_blocked_keywords);
        tvBlockedEmpty = findViewById(R.id.tv_blocked_empty);
        etCustomBlockedChannel = findViewById(R.id.et_custom_blocked_channel);
        cgBlockedChannels = findViewById(R.id.cg_blocked_channels);
        tvBlockedChannelsEmpty = findViewById(R.id.tv_blocked_channels_empty);
        swHideWatched = findViewById(R.id.sw_hide_watched);
        swHideShorts = findViewById(R.id.sw_hide_shorts);
        layoutBlockedCategoriesContainer = findViewById(R.id.layout_blocked_categories_container);

        Button btnAddCustomBlocked = findViewById(R.id.btn_add_custom_blocked);
        btnAddCustomBlocked.setOnClickListener(v -> addCustomBlockedKeyword());

        Button btnAddBlockedChannel = findViewById(R.id.btn_add_blocked_channel);
        btnAddBlockedChannel.setOnClickListener(v -> addCustomBlockedChannel());

        // Populate Interests
        buildPreferredChips();
        buildCategories();

        // Populate Blocked
        buildBlockedChips();
        buildBlockedChannelChips();
        buildBlockedSuggestions();
        buildBlockedCategories();

        // Setup toggles
        swHideWatched.setChecked(db.getHideWatched());
        swHideShorts.setChecked(db.getHideShorts());

        swHideWatched.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.setSetting("hide_watched", isChecked ? "true" : "false");
            Toast.makeText(this, "Hide Watched: " + isChecked, Toast.LENGTH_SHORT).show();
        });

        swHideShorts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.setSetting("hide_shorts", isChecked ? "true" : "false");
            Toast.makeText(this, "Hide Shorts: " + isChecked, Toast.LENGTH_SHORT).show();
        });
    }

    // ==========================================
    // INTERESTS LOGIC
    // ==========================================

    private void buildPreferredChips() {
        cgPreferredTopics.removeAllViews();
        if (preferredTopics.isEmpty()) {
            tvPreferredEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvPreferredEmpty.setVisibility(View.GONE);

        for (String topic : preferredTopics) {
            Chip chip = new Chip(this);
            chip.setText(topic);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1B5E20"))); // Dark green
            chip.setTextColor(Color.WHITE);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.WHITE));

            chip.setOnCloseIconClickListener(v -> {
                removePreferredTopic(topic);
            });
            cgPreferredTopics.addView(chip);
        }
    }

    private void buildCategories() {
        layoutCategoriesContainer.removeAllViews();
        categoryChipsMap.clear();
        categoryCountViewsMap.clear();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (TopicCategory category : CATEGORIES) {
            View cardView = inflater.inflate(R.layout.item_category_card, layoutCategoriesContainer, false);

            TextView tvIcon = cardView.findViewById(R.id.tv_category_icon);
            TextView tvName = cardView.findViewById(R.id.tv_category_name);
            TextView tvCount = cardView.findViewById(R.id.tv_selected_count);
            TextView tvArrow = cardView.findViewById(R.id.tv_arrow);
            LinearLayout header = cardView.findViewById(R.id.category_header);
            LinearLayout content = cardView.findViewById(R.id.category_content);
            ChipGroup cgTopics = cardView.findViewById(R.id.cg_category_topics);

            tvIcon.setText(category.icon);
            tvName.setText(category.name);
            categoryCountViewsMap.put(category.name, tvCount);

            updateCategoryCountText(category);

            header.setOnClickListener(v -> {
                if (content.getVisibility() == View.GONE) {
                    content.setVisibility(View.VISIBLE);
                    tvArrow.setText("▲");
                } else {
                    content.setVisibility(View.GONE);
                    tvArrow.setText("▼");
                }
            });

            // Populate category chips
            for (String topicName : category.topics) {
                Chip chip = new Chip(this);
                chip.setText(topicName);
                chip.setCheckable(false);

                boolean isSelected = preferredTopics.contains(topicName);
                if (isSelected) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2E7D32"))); // green
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C"))); // dark gray
                    chip.setTextColor(Color.parseColor("#BBBBBB"));
                }

                chip.setOnClickListener(v -> {
                    toggleCategoryTopic(topicName, chip, category);
                });

                cgTopics.addView(chip);
                categoryChipsMap.put(topicName, chip);
            }

            layoutCategoriesContainer.addView(cardView);
        }
    }

    private void updateCategoryCountText(TopicCategory category) {
        TextView tvCount = categoryCountViewsMap.get(category.name);
        if (tvCount == null) return;

        int selectedCount = 0;
        for (String topic : category.topics) {
            if (preferredTopics.contains(topic)) {
                selectedCount++;
            }
        }

        if (selectedCount > 0) {
            tvCount.setText(selectedCount + " selected");
            tvCount.setTextColor(Color.parseColor("#4CAF50")); // green
        } else {
            tvCount.setText(category.topics.length + " topics");
            tvCount.setTextColor(Color.parseColor("#888888")); // grey
        }
    }

    private void toggleCategoryTopic(String topicName, Chip chip, TopicCategory category) {
        if (preferredTopics.contains(topicName)) {
            preferredTopics.remove(topicName);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C")));
            chip.setTextColor(Color.parseColor("#BBBBBB"));
        } else {
            preferredTopics.add(topicName);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            chip.setTextColor(Color.WHITE);
        }
        savePreferredTopics();
        buildPreferredChips();
        updateCategoryCountText(category);
    }

    private void addCustomPreferredTopic() {
        String topic = etCustomInterest.getText().toString().trim();
        if (TextUtils.isEmpty(topic)) return;

        if (preferredTopics.contains(topic)) {
            Toast.makeText(this, "Topic already followed", Toast.LENGTH_SHORT).show();
            return;
        }

        preferredTopics.add(topic);
        savePreferredTopics();
        buildPreferredChips();
        etCustomInterest.setText("");

        // If the custom topic belongs to any of the browse category chips, update it to green
        Chip catChip = categoryChipsMap.get(topic);
        if (catChip != null) {
            catChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            catChip.setTextColor(Color.WHITE);
        }

        // Update counts
        for (TopicCategory category : CATEGORIES) {
            for (String t : category.topics) {
                if (t.equalsIgnoreCase(topic)) {
                    updateCategoryCountText(category);
                }
            }
        }
    }

    private void removePreferredTopic(String topic) {
        preferredTopics.remove(topic);
        savePreferredTopics();
        buildPreferredChips();

        // Update browse chip if exists
        Chip catChip = categoryChipsMap.get(topic);
        if (catChip != null) {
            catChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C")));
            catChip.setTextColor(Color.parseColor("#BBBBBB"));
        }

        // Update counts
        for (TopicCategory category : CATEGORIES) {
            for (String t : category.topics) {
                if (t.equalsIgnoreCase(topic)) {
                    updateCategoryCountText(category);
                }
            }
        }
    }

    private void savePreferredTopics() {
        StringBuilder sb = new StringBuilder();
        for (String topic : preferredTopics) {
            sb.append(topic).append("\n");
        }
        db.setSetting("preferred_keywords", sb.toString());
    }

    // ==========================================
    // BLOCKED LOGIC
    // ==========================================

    private void buildBlockedChips() {
        cgBlockedKeywords.removeAllViews();
        if (blockedKeywords.isEmpty()) {
            tvBlockedEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvBlockedEmpty.setVisibility(View.GONE);

        for (String keyword : blockedKeywords) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828"))); // Dark red
            chip.setTextColor(Color.WHITE);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.WHITE));

            chip.setOnCloseIconClickListener(v -> {
                removeBlockedKeyword(keyword);
            });
            cgBlockedKeywords.addView(chip);
        }
    }

    private void buildBlockedChannelChips() {
        cgBlockedChannels.removeAllViews();
        if (blockedChannels.isEmpty()) {
            tvBlockedChannelsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvBlockedChannelsEmpty.setVisibility(View.GONE);

        for (String channel : blockedChannels) {
            Chip chip = new Chip(this);
            chip.setText(channel);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828"))); // Dark red
            chip.setTextColor(Color.WHITE);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.WHITE));

            chip.setOnCloseIconClickListener(v -> {
                blockedChannels.remove(channel);
                saveBlockedChannels();
                buildBlockedChannelChips();
            });
            cgBlockedChannels.addView(chip);
        }
    }

    private void buildBlockedSuggestions() {
        cgBlockedSuggestions.removeAllViews();
        for (String keyword : BLOCKED_SUGGESTIONS) {
            // Only show suggestions that are not currently blocked
            if (blockedKeywords.contains(keyword.toLowerCase())) {
                continue;
            }

            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C"))); // dark gray
            chip.setTextColor(Color.parseColor("#BBBBBB"));

            chip.setOnClickListener(v -> {
                blockedKeywords.add(keyword.toLowerCase());
                saveBlockedKeywords();
                buildBlockedChips();
                buildBlockedSuggestions();

                // Update browse chip if exists
                Chip catChip = blockedCategoryChipsMap.get(keyword.toLowerCase());
                if (catChip != null) {
                    catChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828")));
                    catChip.setTextColor(Color.WHITE);
                }

                // Update counts
                for (TopicCategory category : CATEGORIES) {
                    for (String t : category.topics) {
                        if (t.equalsIgnoreCase(keyword)) {
                            updateBlockedCategoryCountText(category);
                        }
                    }
                }
            });

            cgBlockedSuggestions.addView(chip);
        }
    }

    private void addCustomBlockedKeyword() {
        String keyword = etCustomBlocked.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(keyword)) return;

        if (blockedKeywords.contains(keyword)) {
            Toast.makeText(this, "Keyword already blocked", Toast.LENGTH_SHORT).show();
            return;
        }

        blockedKeywords.add(keyword);
        saveBlockedKeywords();
        buildBlockedChips();
        buildBlockedSuggestions();
        etCustomBlocked.setText("");

        // If the custom topic belongs to any of the browse category chips, update it to red
        Chip catChip = blockedCategoryChipsMap.get(keyword);
        if (catChip != null) {
            catChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828")));
            catChip.setTextColor(Color.WHITE);
        }

        // Update counts
        for (TopicCategory category : CATEGORIES) {
            for (String t : category.topics) {
                if (t.equalsIgnoreCase(keyword)) {
                    updateBlockedCategoryCountText(category);
                }
            }
        }
    }

    private void addCustomBlockedChannel() {
        String channel = etCustomBlockedChannel.getText().toString().trim();
        if (TextUtils.isEmpty(channel)) return;

        if (blockedChannels.contains(channel)) {
            Toast.makeText(this, "Channel already blocked", Toast.LENGTH_SHORT).show();
            return;
        }

        blockedChannels.add(channel);
        saveBlockedChannels();
        buildBlockedChannelChips();
        etCustomBlockedChannel.setText("");
    }

    private void saveBlockedKeywords() {
        StringBuilder sb = new StringBuilder();
        for (String keyword : blockedKeywords) {
            sb.append(keyword).append("\n");
        }
        db.setSetting("blocked_keywords", sb.toString());
    }

    private void saveBlockedChannels() {
        StringBuilder sb = new StringBuilder();
        for (String channel : blockedChannels) {
            sb.append(channel).append("\n");
        }
        db.setSetting("blocked_channels", sb.toString());
    }

    private void buildBlockedCategories() {
        layoutBlockedCategoriesContainer.removeAllViews();
        blockedCategoryChipsMap.clear();
        blockedCategoryCountViewsMap.clear();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (TopicCategory category : CATEGORIES) {
            View cardView = inflater.inflate(R.layout.item_category_card, layoutBlockedCategoriesContainer, false);

            TextView tvIcon = cardView.findViewById(R.id.tv_category_icon);
            TextView tvName = cardView.findViewById(R.id.tv_category_name);
            TextView tvCount = cardView.findViewById(R.id.tv_selected_count);
            TextView tvArrow = cardView.findViewById(R.id.tv_arrow);
            LinearLayout header = cardView.findViewById(R.id.category_header);
            LinearLayout content = cardView.findViewById(R.id.category_content);
            ChipGroup cgTopics = cardView.findViewById(R.id.cg_category_topics);

            tvIcon.setText(category.icon);
            tvName.setText(category.name);
            blockedCategoryCountViewsMap.put(category.name, tvCount);

            updateBlockedCategoryCountText(category);

            header.setOnClickListener(v -> {
                if (content.getVisibility() == View.GONE) {
                    content.setVisibility(View.VISIBLE);
                    tvArrow.setText("▲");
                } else {
                    content.setVisibility(View.GONE);
                    tvArrow.setText("▼");
                }
            });

            // Populate category chips
            for (String topicName : category.topics) {
                Chip chip = new Chip(this);
                chip.setText(topicName);
                chip.setCheckable(false);

                boolean isSelected = blockedKeywords.contains(topicName.toLowerCase());
                if (isSelected) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828"))); // red
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C"))); // dark gray
                    chip.setTextColor(Color.parseColor("#BBBBBB"));
                }

                chip.setOnClickListener(v -> {
                    toggleBlockedCategoryTopic(topicName, chip, category);
                });

                cgTopics.addView(chip);
                blockedCategoryChipsMap.put(topicName.toLowerCase(), chip);
            }

            layoutBlockedCategoriesContainer.addView(cardView);
        }
    }

    private void updateBlockedCategoryCountText(TopicCategory category) {
        TextView tvCount = blockedCategoryCountViewsMap.get(category.name);
        if (tvCount == null) return;

        int selectedCount = 0;
        for (String topic : category.topics) {
            if (blockedKeywords.contains(topic.toLowerCase())) {
                selectedCount++;
            }
        }

        if (selectedCount > 0) {
            tvCount.setText(selectedCount + " blocked");
            tvCount.setTextColor(Color.parseColor("#E53935")); // red
        } else {
            tvCount.setText(category.topics.length + " topics");
            tvCount.setTextColor(Color.parseColor("#888888")); // grey
        }
    }

    private void toggleBlockedCategoryTopic(String topicName, Chip chip, TopicCategory category) {
        String key = topicName.toLowerCase();
        if (blockedKeywords.contains(key)) {
            blockedKeywords.remove(key);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C")));
            chip.setTextColor(Color.parseColor("#BBBBBB"));
        } else {
            blockedKeywords.add(key);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C62828")));
            chip.setTextColor(Color.WHITE);
        }
        saveBlockedKeywords();
        buildBlockedChips();
        buildBlockedSuggestions();
        updateBlockedCategoryCountText(category);
    }

    private void removeBlockedKeyword(String keyword) {
        String key = keyword.toLowerCase();
        blockedKeywords.remove(key);
        saveBlockedKeywords();
        buildBlockedChips();
        buildBlockedSuggestions();

        // Update browse chip if exists
        Chip catChip = blockedCategoryChipsMap.get(key);
        if (catChip != null) {
            catChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C")));
            catChip.setTextColor(Color.parseColor("#BBBBBB"));
        }

        // Update counts
        for (TopicCategory category : CATEGORIES) {
            for (String t : category.topics) {
                if (t.equalsIgnoreCase(keyword)) {
                    updateBlockedCategoryCountText(category);
                }
            }
        }
    }
}
