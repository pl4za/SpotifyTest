package com.pl4za.help;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.pl4za.spotifytest.R;
import com.pl4za.spotifytest.SettingsManager;
import com.pl4za.volley.AppController;

public class CustomListAdapterDrawer extends BaseAdapter {

    private ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static String[] playlists;
    private String user_id, product;
    private Bitmap profilePicture;
    private LayoutInflater inflater;
    private Activity activity;

    public CustomListAdapterDrawer(Activity activity, String[] list, String userId, String product, Bitmap profilePicture) {
        this.playlists = list;
        this.user_id = userId;
        this.activity = activity;
        if (product.equals("open")) {
            this.product = "Spotify free";
        } else if (product.equals("premium")) {
            this.product = "Spotify premium";
        }
        else {
            this.product = "Please login..";
        }
        this.profilePicture = profilePicture;
    }

    @Override
    public int getCount() {
        return playlists.length + 1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (inflater == null)
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int viewType;
        if (getCount() == 1 && position == 0) {
            viewType = TYPE_HEADER;
        } else {
            viewType = getItemViewType(position);
        }
        //if (convertView == null) {
        if (viewType == TYPE_HEADER) {
            convertView = inflater.inflate(R.layout.drawer_list_header, null);
            holder = new ViewHolder(convertView, viewType);
            if (imageLoader == null)
                imageLoader = AppController.getInstance().getImageLoader();
        } else {
            convertView = inflater.inflate(R.layout.drawer_list_item, null);
            holder = new ViewHolder(convertView, viewType);
        }
        convertView.setTag(holder);
        /*
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        */
        if (viewType == TYPE_HEADER) {
            //holder.drawerHeader.setImageUrl("", imageLoader);
            holder.drawerHeader.setDefaultImageResId(R.drawable.drawer_header); // show your drawable
            holder.drawerHeader.setImageUrl(SettingsManager.getInstance().getRandomArtistImage(), imageLoader);
            holder.drawerHeader.setDefaultImageResId(R.drawable.drawer_header);
            holder.drawerHeader.setErrorImageResId(R.drawable.drawer_header);
            holder.userId.setText(user_id);
            holder.product.setText(product);
            holder.profilePic.setImageBitmap(profilePicture);
        } else {
            holder.playlist.setText(playlists[position - 1]);
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    static class ViewHolder {
        TextView playlist, userId, product;
        ImageView profilePic;
        NetworkImageView drawerHeader;

        public ViewHolder(View convertView, int viewType) {
            if (viewType == TYPE_ITEM) {
                playlist = (TextView) convertView.findViewById(R.id.tvPlaylist);
            } else if (viewType == TYPE_HEADER) {
                drawerHeader = (NetworkImageView) convertView.findViewById(R.id.ivArtistImage);
                product = (TextView) convertView.findViewById(R.id.tvProduct);
                userId = (TextView) convertView.findViewById(R.id.tvUser);
                profilePic = (ImageView) convertView.findViewById(R.id.ivProfile);
            }
        }
    }
}
