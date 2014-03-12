import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/*
 * @author Ravi Khadiwala
 */

public class UserTweetsTimeline {
    /**
     * Usage: java twitter4j.examples.timeline.GetUserTimeline
     *
     *Collect the 20 most recent tweets from a set of users
     *
     * @param args String[]
     */
    public static void main(String[] args) {
        // gets Twitter instance with default credentials
        Twitter twitter = new TwitterFactory().getInstance();
        try {
            List<Status> statuses;
            String outfile = "third350.txt";
            String users[] = {"LagunaVistaPD",
            		"AlleghenyBadge",
            		"politiehelmond",
            		"SgtRobVermeulen",
            		"Nashvillepolice",
            		"martinbrunt",
            		"RidgefieldWaPD",
            		"mrsmeaghangray",
            		"petaluma_police",
            		"URPolice",
            		"politie",
            		"TaosPD",
            		"TASERFoundation",
            		"rogernield2703",
            		"GBPolice",
            		"WiredPig",
            		"GPDChief",
            		"merv_pol",
            		"InspTobyDay",
            		"SeaGirtPolice",
            		"ChiefStacey",
            		"WhitehousePd",
            		"showlowpolice",
            		"SWCCNewsNotes",
            		"politiebest",
            		"IsabelleCotton",
            		"TimShieldsBC",
            		"rpdsafercity",
            		"BensalemPD",
            		"kshighwaypatrol",
            		"politieheeze",
            		"Pacific_Police",
            		"RONLEVINE59A1",
            		"CplMoskaluk",
            		"politiewaalre",
            		"politiegeldrop",
            		"occrimescene",
            		"Atlanta_Police",
            		"politieson",
            		"NorwichPolice",
            		"wobable",
            		"TFISHKILLPD",
            		"PolitieOV",
            		"norwellpd",
            		"PSNIBallymena",
            		"thebillboardmag",
            		"Chris_Boarland",
            		"ayymanduh",
            		"ThePBA",
            		"PawleysIslandPD",
            		"WarwickshirePA",
            		"MOD_DMC",
            		"CazCraig",
            		"MHPolice",
            		"WMPVillaFC",
            		"UppStoke_Police",
            		"BobRodkin",
            		"DCIShaunWest",
            		"RCMPNB",
            		"APDCHIEF",
            		"politieaa",
            		"accpolice",
            		"PCSO_Packham",
            		"Placer911",
            		"NZ_Police_Feed",
            		"pcerj",
            		"MiamiBeachPD",
            		"RCSheriff",
            		"CVPolicebablake",
            		"PoliceGrantsHlp",
            		"amandacomms",
            		"LangleyRCMP",
            		"SacFirePIO",
            		"depolitiezoekt",
            		"SanAntonioFires",
            		"LowellPD",
            		"GeaugaSheriff",
            		"RMP_Redcap",
            		"usparkpolicepio",
            		"peel_police",
            		"olliecattermole",
            		"vachiefs",
            		"PolitieTwente",
            		"grayrayner",
            		"ChiefTorigian",
            		"TempePolice",
            		"politiebzo",
            		"BeaverUtSheriff",
            		"LintonPD",
            		"crimereports",
            		"UPDSL",
            		"GCPD",
            		"barrowpolice",
            		"MICO_Sheriff",
            		"PauldingSheriff",
            		"BigBearSheriff",
            		"Brunscosheriff",
            		"WMPBCFC",
            		"politie_frl",
            		"mikebostic",
            		"DPAuthority",
            		"PlaistowPolice",
            		"sheboyganscan",
            		"politieasten",
            		"politielaarbeek",
            		"politiegemert",
            		"UTAustinPolice",
            		"ChestertonPC",
            		"TheConstables",
            		"politiebzozoekt",
            		"CSPChief",
            		"politiebergeijk",
            		"RimonSeed",
            		"AdamMinnion",
            		"carlossteve9251",
            		"PBCRP",
            		"politieregio",
            		"GMPA1",
            		"TPS_31_Division",
            		"SgtHarrisonWMP",
            		"BricePolice",
            		"christammiller",
            		"911EMERGENCYMA",
            		"tombovingdon",
            		"AnnapolisPD",
            		"LeagueCityPD",
            		"PostalInspector",
            		"InspectorWinter",
            		"sdwhite",
            		"SpinneyHillLPU",
            		"SgtJackWest",
            		"InspJForrest",
            		"CALFIRE_PIO",
            		"Ten4Ministries",
            		"ResponsePlod",
            		"safmpmlive",
            		"Whall_Binley",
            		"MHall5544WMP",
            		"politievhv",
            		"SeanHannigan",
            		"bcso911",
            		"MCSOintheknow",
            		"RedlandsPD",
            		"PCSOSkinnerWMP",
            		"BravoEchoOne",
            		"OfficerForestal",
            		"spelling_police",
            		"YumaSheriff",
            		"SgtJamesMain",
            		"NyeSheriff",
            		"CurryCountySO",
            		"Lindon_Police",
            		"YubaSheriff",
            		"alewishamcop",
            		"markhazelby",
            		"SLMPD",
            		"ClevCoSheriff",
            		"PPBTrafficLT",
            		"CEOPUK",
            		"oconeesheriff",
            		"SheriffMarionCo",
            		"KirksvillePD",
            		"PFEWSergeants",
            		"RedondoBeachPD",
            		"TomballPD",
            		"SFStatePD",
            		"Sasha_Taylor",
            		"NambuPoliceA",
            		"NYPFJBB",
            		"santafesheriff",
            		"NationalSheriff"
            		};
            for(int i = 0; i < users.length; i++)
            {
            	statuses = twitter.getUserTimeline(users[i]);
		        System.out.println("Have @" + users[i] + "'s user timeline.");
		        try{
		            BufferedWriter out = new BufferedWriter(new FileWriter(outfile,true));
		            for (Status status : statuses) {
		                out.write(status.getText()+"\n");
		            }
		            out.close();
		        }
		        catch (IOException te){
		        	te.printStackTrace();
		        }
            }
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }
    }
}
