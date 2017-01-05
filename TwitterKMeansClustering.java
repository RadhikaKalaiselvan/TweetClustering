package com.company;

import java.util.*;
import java.io.*;

public class TwitterKMeansClustering {

    public static void main(String[] args) {
        int numClusters = 25;
        String initialSeedsFile = "InitialSeeds.txt", inputFile = "Tweets.json", outputFile = "tweets-k-means-output.txt";
        try {
            numClusters = Integer.parseInt(args[0]);
            initialSeedsFile = args[1];
            inputFile = args[2];
            outputFile = args[3];
        } catch (Exception e) {
            System.out.println("Error reading  program arguments" + e);
        }
        try {
            ArrayList<Tweet> tweets = readTweets(inputFile);
            HashMap<String, Tweet> tweetMap = getTweetMap(tweets);
            ArrayList<Tweet> centroid = getInitialSeeds(initialSeedsFile, tweetMap);
            ArrayList<Cluster> clusters = getClusters(centroid, numClusters);
            kMeans(clusters, tweets, tweetMap);
            print(clusters, outputFile);
            System.out.println("SSE: " + SSE(clusters));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Tweet> readTweets(String inputFile) throws IOException {
        ArrayList<Tweet> tweets = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;
        while ((line = br.readLine()) != null) {
            Tweet tweet = new Tweet();
            String[] token = line.split(",");
            int k = 0;
            while (k < token.length) {
                token[k] = token[k].replace("}", "");
                token[k] = token[k].replace("{", "");
                token[k] = token[k].replaceAll("\"", "");
                String[] tokens = new String[2];
                for (int i = 0; i < token[k].length(); i++) {
                    if (token[k].charAt(i) == ':') {
                        tokens[0] = token[k].substring(0, i);
                        tokens[1] = token[k].substring(i + 1, token[k].length());
                        break;
                    }
                }
                if (tokens[0] == null) {
                    k++;
                    continue;
                }
                if (tokens[0].trim().equalsIgnoreCase("text")) {
                    tweet.msg = tokens[1].trim();
                } else if (tokens[0].trim().equalsIgnoreCase("id")) {
                    tweet.id = tokens[1].trim();
                }
                k++;
            }
            tweets.add(tweet);
        }
        br.close();
        return tweets;
    }

    private static ArrayList<Tweet> getInitialSeeds(String initialSeedsFile, HashMap<String, Tweet> tweetMap) throws Exception {
        ArrayList<String> initialseeds = new ArrayList<>();
        ArrayList<Tweet> initialSeedList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(initialSeedsFile));
        String line;
        while ((line = br.readLine()) != null) {
            String[] input_Token = line.split(",");
            for (String token : input_Token) {
                initialseeds.add(token.trim());
            }
        }
        br.close();
        for (int k = 0; k < initialseeds.size(); k++) {
            initialSeedList.add(tweetMap.get(initialseeds.get(k)));
        }
        return initialSeedList;
    }


    private static void kMeans(ArrayList<Cluster> clusters, ArrayList<Tweet> tweets, HashMap<String, Tweet> tweetMap) {

        boolean converge = true;

        for (int iterations = 0; converge && iterations < 25; iterations++) {
            int index = 0;
            converge = false;
            while (index < tweets.size()) {
                Cluster cluster = jaccardDistance(tweets.get(index), clusters);
                cluster.tweets.add(tweets.get(index));
                index++;
            }
            for (int i = 0; i < clusters.size(); i++) {
                Tweet tweet1;
                TreeMap<String, Integer> treeMap = new TreeMap<>(Collections.reverseOrder());
                Cluster cluster = clusters.get(i);
                ArrayList<Tweet> tweetList = cluster.tweets;
                for (int j = 0; j < tweetList.size(); j++) {
                    double min = 9999;
                    Tweet t1 = tweetList.get(j);
                    Tweet minTweet = null;
                    for (Tweet tweet2 : cluster.tweets) {
                        if (!tweet2.id.trim().equals(t1.id.trim())) {
                            double jaccardDistance = getJaccardDistance(t1.msg.trim(), tweet2.msg.trim());
                            if (jaccardDistance <= min) {
                                minTweet = tweet2;
                                min = jaccardDistance;
                            }
                        }
                    }
                    tweet1 = minTweet;
                    if (treeMap.containsKey(tweet1.id.trim())) {
                        Integer c = treeMap.get(tweet1.id);
                        treeMap.remove(tweet1.id.trim());
                        treeMap.put(tweet1.id.trim(), c + 1);
                    } else {
                        treeMap.put(tweet1.id.trim(), 1);
                    }
                }
                String id = treeMap.firstKey();
                if (!cluster.centroidTweet.id.trim().equals(id)) {
                    cluster.centroidTweet = tweetMap.get(id);
                    converge = true;
                }
            }

            if (converge) {
                for (int i = 0; i < clusters.size(); i++) {
                    Cluster cluster = clusters.get(i);
                    cluster.tweets.clear();
                }

            }
        }

    }

    private static Cluster jaccardDistance(Tweet tweet, ArrayList<Cluster> clusters) {
        TreeMap<Double, Cluster> treeMap = new TreeMap<>();
        for (Cluster cluster : clusters) {
            double jaccardDistance = getJaccardDistance(tweet.msg, cluster.centroidTweet.msg);
            treeMap.put(jaccardDistance, cluster);
        }
        return treeMap.get(treeMap.firstKey());
    }

    private static double SSE(ArrayList<Cluster> clusters) {
        double SSE = 0;
        for (Cluster cluster : clusters) {
            for (Tweet tweet : cluster.tweets) {
                SSE += Math.pow(getJaccardDistance(tweet.msg.trim(), cluster.centroidTweet.msg.trim()), 2);
            }
        }
        return SSE;
    }

    private static String trimTweets(String tweet) {
        tweet.replaceAll("[^\\w\\s]", "");
        tweet.replaceAll("RT", "");
        return tweet;
    }

    private static HashMap<String, Integer> getTweetCount(String[] tweet) {
        HashMap<String, Integer> tweetCount = new HashMap<>();
        for (int i = 0; i < tweet.length; i++) {

            String word = tweet[i];
            word = word.replaceAll(" ,$", "");
            if (tweetCount.containsKey(word)) {
                Integer c = tweetCount.get(word);
                c++;
                tweetCount.remove(word);
                tweetCount.put(word, c);
            } else {
                tweetCount.put(word, 1);
            }
        }
        return tweetCount;
    }

    public static double getJaccardDistance(String text1, String text2) {

        text1 = trimTweets(text1);
        text2 = trimTweets(text2);
        String[] t1 = text1.trim().split("\\s+");
        String[] t2 = text2.trim().split("\\s+");
        HashMap<String, Integer> tweet1Count = getTweetCount(t1);
        HashMap<String, Integer> tweet2Count = getTweetCount(t2);
        int intersection = 0;
        for (Map.Entry<String, Integer> entry : tweet2Count.entrySet()) {
            if (tweet1Count.containsKey(entry.getKey())) {
                if (tweet1Count.get(entry.getKey()).equals(entry.getValue())) {
                    intersection++;
                }
            }
        }
        double union = tweet1Count.size() + tweet2Count.size() - intersection;
        return 1 - (intersection / union);
    }

    public static HashMap<String, Tweet> getTweetMap(ArrayList<Tweet> tweets) {
        HashMap<String, Tweet> tweetMap = new HashMap<>();
        for (int k = 0; k < tweets.size(); k++) {
            Tweet tweet = tweets.get(k);
            tweetMap.put(tweet.id.trim(), tweet);
        }
        return tweetMap;
    }

    public static ArrayList<Cluster> getClusters(ArrayList<Tweet> centroid, int numClusters) {
        ArrayList<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < centroid.size(); i++) {
            Cluster cluster = new Cluster();
            cluster.id = i + 1;
            cluster.centroidTweet = centroid.get(i);
            if (centroid.get(i) != null) {
                cluster.tweets.add(centroid.get(i));
            }
            clusters.add(cluster);
            if (i == numClusters) {
                break;
            }
        }
        return clusters;
    }

    public static void print(ArrayList<Cluster> clusters, String outputFile) throws IOException {
        File file = new File(outputFile);
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        for (int k = 0; k < clusters.size(); k++) {
            Cluster cluster = clusters.get(k);
            bufferedWriter.write("\n" + cluster.id + ":\t");
            for (int j = 0; j < cluster.tweets.size(); j++) {
                bufferedWriter.write(cluster.tweets.get(j).id);
                if (j != cluster.tweets.size() - 1) {
                    bufferedWriter.write(", ");
                }
            }
            bufferedWriter.write("\n");

        }
        bufferedWriter.write("SSE: " + SSE(clusters));
        bufferedWriter.close();
    }
}