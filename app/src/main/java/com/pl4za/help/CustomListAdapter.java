package com.pl4za.help;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.spotlight.R;
import com.pl4za.spotlight.Track;
import com.pl4za.volley.AppController;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class CustomListAdapter extends RecyclerSwipeAdapter<CustomListAdapter.ViewHolder> implements Filterable {
    private static final String TAG = "CustomListAdapter";
    private List<Track> trackList, originalTracklist;
    private static ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private FragmentOptions swipeListener;
    private String direction = "right";

    public CustomListAdapter(List<Track> trackList) {
        this.trackList = trackList;
    }

    public void setSwipeListener(FragmentOptions i) {
        this.swipeListener = i;
    }

    public void setSwipeDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public int getItemCount() {
        try {
            return trackList.size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    private String convertAdded(String time) {
        DateTimeFormatter originalFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTime dateAdded = originalFormat.parseDateTime(time);
        DateTime currentDate = new DateTime();
        // joda
        Hours hoursAgo = Hours.hoursBetween(dateAdded, currentDate);
        Days daysAgo = Days.daysBetween(dateAdded, currentDate);
        Months MonthsAgo = Months.monthsBetween(dateAdded, currentDate);
        Years YearsAgo = Years.yearsBetween(dateAdded, currentDate);
        //System.out.println(dateAdded + " - " + currentDate + " = " + daysAgo.getDays());
        if (YearsAgo.getYears() > 0)
            return "Added " + YearsAgo.getYears() + " years ago";
        else if (MonthsAgo.getMonths() > 0)
            return "Added " + MonthsAgo.getMonths() + " months ago";
        else if (daysAgo.getDays() > 0)
            return "Added " + daysAgo.getDays() + " days ago";
        else
            return "Added " + hoursAgo.getHours() + " hours ago";
    }

    private String convertTime(String added) {
        Long millis = Long.parseLong(added);
        Long seconds = (millis / 1000) % 60;
        String finalSeconds;
        if ((seconds / 10) < 1) {
            finalSeconds = "0" + seconds.toString();
        } else
            finalSeconds = seconds.toString();
        Long minutes = ((millis - seconds) / 1000) / 60;
        return minutes + ":" + finalSeconds;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                trackList = (List<Track>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                return getFilteredResults(constraint);
            }

            private FilterResults getFilteredResults(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final ArrayList<Track> results = new ArrayList<>();
                if (originalTracklist == null)
                    originalTracklist = trackList;
                if (constraint != null && !constraint.toString().equals("")) {
                    if (originalTracklist != null && originalTracklist.size() > 0) {
                        for (int i = 0; i < originalTracklist.size(); i++) {
                            if (originalTracklist.get(i).getTrack().toLowerCase().contains(constraint.toString())) { //Track
                                results.add(originalTracklist.get(i));
                            }
                            else if (originalTracklist.get(i).getAlbum().toLowerCase().contains(constraint.toString())) { //Album
                                results.add(originalTracklist.get(i));
                            }
                            else if (originalTracklist.get(i).getSimpleArtist().toLowerCase().contains(constraint.toString())) { //Artist
                                results.add(originalTracklist.get(i));
                            }
                        }
                    }
                    oReturn.values = results;
                } else
                    oReturn.values = originalTracklist;
                swipeListener.setList((List<Track>) oReturn.values);
                return oReturn;
            }
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_view_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Track track = trackList.get(position);
        if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        String albumArt = track.getAlbumArt();
        if (!track.getAlbumArt().equals(""))
            holder.thumbNail.setImageUrl(albumArt, imageLoader);
        holder.track.setText(track.getTrack());
        String[] artists = track.getArtist();
        String artistText = "";
        for (int i = 0; i < artists.length; i++) {
            artistText = artists[i];
            if (i + 1 < artists.length)
                artistText += " - ";
        }
        holder.artist.setText(artistText);
        holder.time.setText(convertTime(track.getTime()));
        holder.album.setText(track.getAlbum());
        holder.added.setText(convertAdded(track.getAdded()));
        imageLoader.get(albumArt, ImageLoader.getImageListener(
                holder.thumbNail, R.drawable.no_image, R.drawable.no_image));
        if (direction.equals("right")) {
            holder.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, holder.swipeLayout.findViewById(R.id.back));
        } else {
            holder.swipeLayout.addDrag(SwipeLayout.DragEdge.Right, holder.swipeLayout.findViewById(R.id.back));
        }
        holder.swipeLayout.addSwipeListener(new SwipeListener(position));
        holder.swipeLayout.setOnDoubleClickListener(new DoubleClickListenter(position));
    }

    class SwipeListener extends SimpleSwipeListener {

        final int position;

        SwipeListener(int pos) {
            this.position = pos;
        }

        @Override
        public void onOpen(SwipeLayout layout) {
            layout.close(true);
            swipeListener.onSwipe(position);
        }

    }

    class DoubleClickListenter implements SwipeLayout.DoubleClickListener {

        final int position;

        DoubleClickListenter(int pos) {
            this.position = pos;
        }

        @Override
        public void onDoubleClick(SwipeLayout swipeLayout, boolean b) {
            swipeListener.onDoubleClick(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final FixedSwipeLayout swipeLayout;
        final NetworkImageView thumbNail;
        final TextView track;
        final TextView artist;
        final TextView time;
        final TextView album;
        final TextView added;

        public ViewHolder(View itemView) {
            super(itemView);
            swipeLayout = (FixedSwipeLayout) itemView.findViewById(R.id.swipe);
            thumbNail = (NetworkImageView) itemView.findViewById(R.id.thumbnail);
            track = (TextView) itemView.findViewById(R.id.track);
            artist = (TextView) itemView.findViewById(R.id.artist);
            time = (TextView) itemView.findViewById(R.id.time);
            album = (TextView) itemView.findViewById(R.id.album);
            added = (TextView) itemView.findViewById(R.id.added);
        }
    }
}
