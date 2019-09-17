package com.example.catty.comp448_assignmentthree;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>{

    private List<Marker> list;

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        public LinearLayout disView;

        public MyViewHolder(LinearLayout v){
            super(v);
            this.disView = v;
        }
    }
    public MyAdapter(List<Marker> markers){
        this.list = markers;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_view, viewGroup, false);
        return new MyViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        Marker marker = list.get(i);

        TextView textView1 = (TextView) myViewHolder.itemView.findViewById(R.id.markerName);
        TextView textView2 = (TextView)myViewHolder.itemView.findViewById(R.id.distance);

        textView1.setText(marker.getTitle());
        textView2.setText(marker.getSnippet());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
