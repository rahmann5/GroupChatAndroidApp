package com.example.naziur.groupchatandroidapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import java.util.ArrayList;

import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;

public class CustomAdapter extends ArrayAdapter<String> {

    private ArrayList<String> dataSet;
    Context mContext;

    public CustomAdapter(ArrayList<String> messages, Context context) {
        super(context, R.layout.list_item_chat_message, messages);
        mContext = context;
        dataSet = messages;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String message = getItem(position);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_chat_message, parent, false);
            EmojiconTextView messageTv = convertView.findViewById(R.id.message);
            messageTv.setText(message);
            if (messageTv.getText().toString().split(":").length == 1){
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.END;
                messageTv.setLayoutParams(params);
            }
        }
        return convertView;
    }
}
