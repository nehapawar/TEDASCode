//package crawler;
 
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
 
import twitter4j.User;
 
public class UserStatusField {
    public static final String USERID = "ID";
    public static final String NAME = "NAME";
    public static final String LOCATION = "LOCATION";
    public static final String STATUS_COUNT = "count";
    public static final String DESCRIPTION = "description";
    public static final String FRIENDS ="friendCount";
    public static final String FOLLOWERS = "followers";
    public static final String AGE = "age";
    public static final String FAVOURAITES = "favoraites";
     
     
    public static void  writeUser(BufferedWriter out, User u) {
        try {
            out.write(USERID + "\t"+u.getId()+"\n");
            //Write the user ID
               
            out.write(NAME + "\t"+ u.getScreenName() +"\n");
            //Write the ScreenName of the user
         
            //Write the StatusCount of the user
            int statusCount = u.getStatusesCount();
            out.write("StatusCount:\t" + statusCount + "\n");
             
            //Write the Age of the user;
            Date createdAt = u.getCreatedAt();
            out.write("Age:\t" + createdAt.toGMTString() + "\n");
             
             
            //Write the Age of the user;
            int faveratesCount = u.getFavouritesCount();
            out.write("FavariatesCount:\t" + faveratesCount + "\n");
             
            //int faveratesCount = u.getFavouritesCount();
            out.write("URL:\t" + u.getURL() + "\n");
             
             
            String followers_num=String.format("Followers: %d%n",u.getFollowersCount());
            out.write(followers_num);
            //Number of followers
                         
            String friends_count=String.format("Friends:\t%d%n",u.getFriendsCount());
            out.write(friends_count);
            //Number of friends(Following)
             
            out.write("Location: "+u.getLocation()+"\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }      
         
    }
}
