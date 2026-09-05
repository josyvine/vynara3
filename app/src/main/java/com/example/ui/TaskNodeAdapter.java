package com.example.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.tasks.TaskNode;

import java.util.ArrayList;
import java.util.List;

public class TaskNodeAdapter extends RecyclerView.Adapter<TaskNodeAdapter.ViewHolder> {

    private final List<TaskNode> taskList = new ArrayList<>();

    public void setTasks(List<TaskNode> tasks) {
        this.taskList.clear();
        if (tasks != null) {
            this.taskList.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_node, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskNode task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());

        TaskNode.Status status = task.getStatus();
        holder.tvStatusBadge.setText(status.name());

        switch (status) {
            case COMPLETED:
                holder.tvIcon.setText("✓");
                holder.tvIcon.setTextColor(Color.parseColor("#00E676"));
                holder.tvStatusBadge.setTextColor(Color.parseColor("#00E676"));
                break;
            case RUNNING:
                holder.tvIcon.setText("●");
                holder.tvIcon.setTextColor(Color.parseColor("#00E5FF"));
                holder.tvStatusBadge.setTextColor(Color.parseColor("#00E5FF"));
                break;
            case FAILED:
                holder.tvIcon.setText("✕");
                holder.tvIcon.setTextColor(Color.parseColor("#FF5252"));
                holder.tvStatusBadge.setTextColor(Color.parseColor("#FF5252"));
                break;
            default:
                holder.tvIcon.setText("○");
                holder.tvIcon.setTextColor(Color.parseColor("#686D88"));
                holder.tvStatusBadge.setTextColor(Color.parseColor("#686D88"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvDesc, tvStatusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_task_icon);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDesc = itemView.findViewById(R.id.tv_task_desc);
            tvStatusBadge = itemView.findViewById(R.id.tv_task_status_badge);
        }
    }
}
