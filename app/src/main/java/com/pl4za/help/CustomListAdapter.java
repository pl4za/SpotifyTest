package com.pl4za.help;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.pl4za.interfaces.ISwipeListener;
import com.pl4za.spotifytest.FragmentQueue;
import com.pl4za.spotifytest.FragmentTracks;
import com.pl4za.spotifytest.MainActivity;
import com.pl4za.spotifytest.R;
import com.pl4za.spotifytest.Track;
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

public class CustomListAdapter extends BaseSwipeAdapter implements Filterable {
    private static final String TAG = "CustomListAdapter";
    private List<Track> trackList;
    private ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private List<Track> originalTracklist = null;
    private final Activity activity;
    private LayoutInflater inflater;
    private ISwipeListener swipeListener;
    private String direction = "right";

    public CustomListAdapter(Activity activity, List<Track> trackList) {
        this.activity = activity;
        this.trackList = trackList;
    }

    public void setSwipeListener(ISwipeListener i) {
        this.swipeListener = i;
    }

    public void setSwipeDirection(String direction) {this.direction = direction;}

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(final int position, ViewGroup parent) {
        View v = LayoutInflater.from(activity).inflate(R.layout.list_view_item, null);
        final SwipeLayout swipeLayout = (SwipeLayout) v.findViewById(getSwipeLayoutResourceId(position));
        if (direction.equals("right")) {
            swipeLayout.addDrag(SwipeLayout.DragEdge.Left, swipeLayout.findViewById(R.id.back));
        } else {
            swipeLayout.addDrag(SwipeLayout.DragEdge.Right, swipeLayout.findViewById(R.id.back));
        }
        swipeLayout.addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onOpen(SwipeLayout layout) {

                swipeListener.onSwipe(position);
                layout.close();
            }
        });
        swipeLayout.setOnDoubleClickListener(new SwipeLayout.DoubleClickListener() {
            @Override
            public void onDoubleClick(SwipeLayout layout, boolean surface) {
                swipeListener.onDoubleClick(position);
            }
        });
        return v;
    }

    @Override
    public int getCount() {
        return trackList.size();
    }

    @Override
    public Object getItem(int location) {
        return trackList.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void fillValues(int position, View convertView) {
        ViewHolder holder = new ViewHolder();
        holder.thumbNail = (NetworkImageView) convertView.findViewById(R.id.thumbnail);
        holder.track = (TextView) convertView.findViewById(R.id.track);
        holder.artist = (TextView) convertView.findViewById(R.id.artist);
        holder.time = (TextView) convertView.findViewById(R.id.time);
        holder.album = (TextView) convertView.findViewById(R.id.album);
        holder.added = (TextView) convertView.findViewById(R.id.added);
        if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        // getting movie data for the row
        Track trackInfo = trackList.get(position);
        String albumArt = trackInfo.getAlbumArt();
        // thumbnail image
        if (!trackInfo.getAlbumArt().equals(""))
            holder.thumbNail.setImageUrl(albumArt, imageLoader);
        // Text info
        holder.track.setText(trackInfo.getTrack());
        String[] artists = trackInfo.getArtist();
        String artistText = "";
        for (int i = 0; i < artists.length; i++) {
            artistText = artists[i];
            if (i + 1 < artists.length)
                artistText += " - ";
        }
        holder.artist.setText(artistText);
        holder.time.setText(convertTime(trackInfo.getTime()));
        holder.album.setText(trackInfo.getAlbum());
        holder.added.setText(convertAdded(trackInfo.getAdded()));
        // Loading image with placeholder and error image
        imageLoader.get(albumArt, ImageLoader.getImageListener(
                holder.thumbNail, R.drawable.no_image, R.drawable.no_image));
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
                FilterResults filteredResults = getFilteredResults(constraint);
                return filteredResults;
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
                            if (originalTracklist.get(i).getAlbum().toLowerCase().contains(constraint.toString())) { //Album
                                results.add(originalTracklist.get(i));
                            }
                            if (originalTracklist.get(i).getSimpleArtist().toLowerCase().contains(constraint.toString())) { //Artist
                                results.add(originalTracklist.get(i));
                            }
                        }
                    }
                    oReturn.values = results;
                } else
                    oReturn.values = originalTracklist;
                if (MainActivity.currentPage == 0) {
                    FragmentTracks.updateTrackList((List<Track>) oReturn.values);
                } else if (MainActivity.currentPage == 1) {
                    FragmentQueue.setFilteredList((List<Track>) oReturn.values);
                }
                return oReturn;
            }
        };
    }

    static class ViewHolder {
        NetworkImageView thumbNail;
        TextView track;
        TextView artist;
        TextView time;
        TextView album;
        TextView added;
    }
}
