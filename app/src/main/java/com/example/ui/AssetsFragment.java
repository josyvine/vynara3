package com.example.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.MainActivity;
import com.example.R;
import com.example.asset.Asset;
import com.example.asset.AssetManager;
import com.example.runtime.ProjectRuntime;

import java.util.ArrayList;
import java.util.List;

public class AssetsFragment extends Fragment {

    private RecyclerView rvAssets;
    private AssetAdapter adapter;
    private final List<AssetItem> displayedAssets = new ArrayList<>();
    private final List<Asset> realAssets = new ArrayList<>();
    private EditText etSearch;
    private ProjectRuntime runtime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Phase 1 Alignment: Retrieve shared ProjectRuntime instance
        if (getActivity() instanceof MainActivity) {
            runtime = ((MainActivity) getActivity()).getProjectRuntime();
        } else {
            runtime = ProjectRuntime.getInstance(requireContext());
        }

        rvAssets = view.findViewById(R.id.rv_assets);
        etSearch = view.findViewById(R.id.et_search_assets);

        rvAssets.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new AssetAdapter(assetItem -> {
            if (assetItem == null) return;

            // Phase 15 Alignment: Inject selected 3D asset model into active studio scene graph
            boolean success = runtime.injectAssetIntoActiveScene(assetItem.getId());
            if (success) {
                Toast.makeText(getContext(), "Added " + assetItem.getName() + " to Studio Scene", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToStudio();
                }
            } else {
                Toast.makeText(getContext(), "Failed to add asset to scene", Toast.LENGTH_SHORT).show();
            }
        });
        rvAssets.setAdapter(adapter);

        loadGeneratedAssets();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterAssets(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        setupCategoryChips(view);
    }

    /**
     * Phase 15 Alignment: Loads the active 3D asset models dynamically
     * from ProjectRuntime instead of reading hardcoded mock default rows.
     */
    private void loadGeneratedAssets() {
        displayedAssets.clear();
        realAssets.clear();

        AssetManager assetMgr = runtime.getAssetManager();
        List<Asset> localAssets = assetMgr.getAssets();

        if (localAssets != null && !localAssets.isEmpty()) {
            realAssets.addAll(localAssets);
            for (Asset a : localAssets) {
                String icon = getCategoryEmoji(a.getCategory());
                String details = a.getFileSizeStr() + " • " + a.getFormat();
                if (a.getPolyCount() > 0) {
                    details = a.getPolyCount() + " tris • " + details;
                }
                
                AssetItem item = new AssetItem(a.getId(), a.getName(), a.getCategory(), icon, details);
                displayedAssets.add(item);
            }
        } else {
            // Populate basic default template assets if local storage is clean
            populateFallbackDefaultTemplates(assetMgr);
            loadGeneratedAssets();
            return;
        }

        adapter.setAssets(displayedAssets);
    }

    private void populateFallbackDefaultTemplates(AssetManager assetMgr) {
        assetMgr.addAsset(new Asset("a1", "Modern Villa Structure", "Architecture", "GLTF", "24.5 MB", "", 24500));
        assetMgr.addAsset(new Asset("a2", "Rigged Biped Hero", "Character", "GLTF", "18.2 MB", "", 18200));
        assetMgr.addAsset(new Asset("a3", "Animated Quadruped Dog", "Creature", "GLTF", "8.4 MB", "", 8400));
        assetMgr.addAsset(new Asset("a4", "Beveled Leather Sofa", "Furniture", "GLTF", "3.2 MB", "", 3200));
        assetMgr.addAsset(new Asset("a5", "Procedural Oak Tree", "Vegetation", "GLTF", "1.8 MB", "", 1800));
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        String cat = category.toUpperCase().trim();
        switch (cat) {
            case "CHARACTER": return "🦸";
            case "CREATURE": return "🐕";
            case "ARCHITECTURE": return "🏠";
            case "FURNITURE": return "🛋️";
            case "VEGETATION": case "ENVIRONMENT": return "🌴";
            case "VEHICLE": return "🏎️";
            default: return "📦";
        }
    }

    private void filterAssets(String query) {
        if (query == null || query.isEmpty()) {
            adapter.setAssets(displayedAssets);
            return;
        }

        List<AssetItem> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (AssetItem item : displayedAssets) {
            if ((item.getName() != null && item.getName().toLowerCase().contains(q)) ||
                (item.getCategory() != null && item.getCategory().toLowerCase().contains(q))) {
                filtered.add(item);
            }
        }
        adapter.setAssets(filtered);
    }

    private void setupCategoryChips(View root) {
        View chipAll = root.findViewById(R.id.chip_cat_all);
        View chipChar = root.findViewById(R.id.chip_cat_characters);
        View chipObj = root.findViewById(R.id.chip_cat_objects);
        View chipBuild = root.findViewById(R.id.chip_cat_buildings);
        View chipMat = root.findViewById(R.id.chip_cat_materials);

        if (chipAll != null) chipAll.setOnClickListener(v -> adapter.setAssets(displayedAssets));
        if (chipChar != null) chipChar.setOnClickListener(v -> filterAssets("Character"));
        if (chipObj != null) chipObj.setOnClickListener(v -> filterAssets("Furniture"));
        if (chipBuild != null) chipBuild.setOnClickListener(v -> filterAssets("Architecture"));
        if (chipMat != null) chipMat.setOnClickListener(v -> filterAssets("Vehicle"));
    }
}