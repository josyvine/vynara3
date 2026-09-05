package com.example.ui;

import android.graphics.Color;
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

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(ProjectItem project);
    }

    private final List<ProjectItem> projects = new ArrayList<>();
    private OnProjectClickListener listener;

    public ProjectAdapter(OnProjectClickListener listener) {
        this.listener = listener;
    }

    public void setProjects(List<ProjectItem> newProjects) {
        this.projects.clear();
        if (newProjects != null) {
            this.projects.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectItem item = projects.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrompt.setText(item.getPrompt());
        holder.tvStatus.setText(item.getStatus());
        holder.tvInfo.setText(item.getInfo());

        if ("COMPLETED".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676"));
        } else if ("IN_PROGRESS".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#A0A5BD"));
        }

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvPrompt, tvInfo;
        Button btnOpen;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_project_name);
            tvStatus = itemView.findViewById(R.id.tv_item_project_status);
            tvPrompt = itemView.findViewById(R.id.tv_item_project_prompt);
            tvInfo = itemView.findViewById(R.id.tv_item_project_info);
            btnOpen = itemView.findViewById(R.id.btn_item_open_project);
        }
    }
}
