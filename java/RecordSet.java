/**
 * @(#) RecordSet.java
 */

import java.util.*;
import java.awt.*;

/*
  Class implementing the winner & record system
 */

public class RecordSet {

    public class Record{
        public Record(Player owner){ m_player = owner; }
        public Player   m_player = null;
        public int      m_kills = 0;     // number of kill a player made
        public int      m_deaths = 0;    // number of times the player died
        public int      m_shotsMade = 0; // number of bullets player shot
        public int      m_shotsHit = 0;  // number of bullets that hit a target
    }

    public enum WinPlace{
        FIRST,
        SECOND,
        THIRD
    };

    // colors for the winner bots
    static Color  DEFAULT_USER_COLOR = new Color(77, 109, 243);
    static Color  DEFAULT_COLOR = Color.GREEN;
    static Color  PLACE_1st_COLOR = Color.YELLOW;
    static Color  PLACE_2nd_COLOR = Color.LIGHT_GRAY;
    static Color  PLACE_3rd_COLOR = new Color(215, 100, 0);
    static int    MAX_ROUND_KILLS = 15;

    private HashMap<Player, Record>   m_records = new HashMap<Player, Record>();

    public RecordSet(){ }

    // reset all records
    public void reset(){
        m_records = new HashMap<Player, Record>();
    }

    // returns true if this round is over
    public boolean isRoundDone()
    {
        Player best = this.getMostKills(WinPlace.FIRST);
        if(best != null && getRecord(best).m_kills >= MAX_ROUND_KILLS){
            return true;
        }
        return false;
    }

    // returns a the records for a specified player or null - if not found
    public Record getRecord(Player player)
    {
        if(m_records.containsKey(player)) return m_records.get(player);
        return null;
    }

    // returns the player that made the most kills by place, or null if there is none
    // returns null too if all players havent got a kill
    public Player getMostKills(WinPlace place)
    {
        ArrayList<Record> list = new ArrayList<Record>(m_records.values());

        Collections.sort(list, new Comparator<Record>(){
            public int compare(Record a, Record b){
                if(a.m_kills > b.m_kills) return -1; // a is better
                else if(a.m_kills < b.m_kills) return 1; // b is better

                // when the kill are equal, test by deaths count
                if(a.m_deaths < b.m_deaths) return -1; // a is better
                else if(a.m_deaths > b.m_deaths) return 1; // b is better
                
                return 0; // totally equal
            }
        });

        // test if all players have the same amount of kills = 0
        boolean allSame = true;
        for(int i=0; i<list.size(); i++){
            if(list.get(i).m_kills != 0){
                allSame = false;
                break;
            }
        }

        // no one scored a point yet,
        // and we cant give medals for a zero score!
        if(allSame) return null; 

        if(list.size() > 0 && place == WinPlace.FIRST) return list.get(0).m_player;
        if(list.size() > 1 && place == WinPlace.SECOND) return list.get(1).m_player;
        if(list.size() > 2 && place == WinPlace.THIRD) return list.get(2).m_player;

        return null;
    }


    ///////////////////////////////////////////////////
    // counting utilities
    ///////////////////////////////////////////////////
    public void addShot(Player owner){
        if(m_records.containsKey(owner)){
            m_records.get(owner).m_shotsMade += 1;
        }else{
            Record r = new Record(owner);
            r.m_shotsMade += 1;
            m_records.put(owner, r);
        }
    }

    public void addHit(Player owner){
        if(m_records.containsKey(owner)){
            m_records.get(owner).m_shotsHit += 1;
        }else{
            Record r = new Record(owner);
            r.m_shotsHit += 1;
            m_records.put(owner, r);
        }
    }

    public void addKill(Player owner){
        if(m_records.containsKey(owner)){
            m_records.get(owner).m_kills += 1;
        }else{
            Record r = new Record(owner);
            r.m_kills += 1;
            m_records.put(owner, r);
        }
    }

    public void addDeath(Player owner){
        if(m_records.containsKey(owner)){
            m_records.get(owner).m_deaths += 1;
        }else{
            Record r = new Record(owner);
            r.m_deaths += 1;
            m_records.put(owner, r);
        }
    }
}
