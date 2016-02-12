package com.pl4za.help;

import com.pl4za.spotifast.Track;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ListComparator implements Comparator<Track> {

    public int compare(Track first, Track second) {
        Date firstDate;
        Date secondDate;
        try {
            // MON FEB 03 15:40:00 WET 2014
            firstDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.ENGLISH).parse(first.getAdded());
            secondDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.ENGLISH).parse(second.getAdded());
            return secondDate.compareTo(firstDate);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

}