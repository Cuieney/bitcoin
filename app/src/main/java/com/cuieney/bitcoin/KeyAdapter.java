package com.cuieney.bitcoin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeyAdapter extends RecyclerView.Adapter<KeyAdapter.ViewHolder> {
    List<String> mnemonicCode;
    Context context;

    public KeyAdapter(List<String> mnemonicCode, Context context) {
        this.mnemonicCode = mnemonicCode;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_key_word, parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.key.setText(mnemonicCode.get(position));
    }

    @Override
    public int getItemCount() {
        return mnemonicCode.size();
    }

   static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView key;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            key = ((TextView) itemView.findViewById(R.id.key_word));
        }


    }
}
