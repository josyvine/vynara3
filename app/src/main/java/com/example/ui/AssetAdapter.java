package com.example.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.ViewHolder> {

    public interface OnAssetClickListener {
        void onAssetClick(AssetItem asset);
    }

    private final List<AssetItem> assets = new ArrayList<>();
    private OnAssetClickListener listener;

    public AssetAdapter(OnAssetClickListener listener) {
        this.listener = listener;
    }

    public void setAssets(List<AssetItem> newAssets) {
        this.assets.clear();
        if (newAssets != null) {
            this.assets.addAll(newAssets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetItem asset = assets.get(position);
        holder.tvIcon.setText(asset.getIcon());
        holder.tvName.setText(asset.getName());
        holder.tvCategory.setText(asset.getCategory() + " • " + asset.getDetails());

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssetClick(asset);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssetClick(asset);
            }
        });
    }

    @Override
    public int getItemCount() {
        return assets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvCategory;
        Button btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_asset_icon);
            tvName = itemView.findViewById(R.id.tv_asset_name);
            tvCategory = itemView.findViewById(R.id.tv_asset_category);
            btnAction = itemView.findViewById(R.id.btn_asset_action);
        }
    }
}
