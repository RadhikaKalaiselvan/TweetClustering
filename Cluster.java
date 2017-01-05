package com.company;

import java.util.ArrayList;

class Cluster {
    int id;
    Tweet centroidTweet;
    ArrayList<Tweet> tweets;

    Cluster() {
        centroidTweet = new Tweet();
        id = -1;
        tweets = new ArrayList<>();

    }
}
