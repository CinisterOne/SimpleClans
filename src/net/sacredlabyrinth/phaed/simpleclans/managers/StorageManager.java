package net.sacredlabyrinth.phaed.simpleclans.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.data.DBCore;
import net.sacredlabyrinth.phaed.simpleclans.data.MySQLCore;
import net.sacredlabyrinth.phaed.simpleclans.data.SQLiteCore;

/**
 *
 * @author phaed
 */
public final class StorageManager
{
    private SimpleClans plugin;
    /**
     *
     */
    public DBCore core;

    /**
     *
     * @param plugin
     */
    public StorageManager()
    {
        plugin = SimpleClans.getInstance();
        initiateDB();
        importFromDatabase();
    }

    /**
     * Initiates the db
     */
    public void initiateDB()
    {
        if (plugin.getSettingsManager().isUseMysql())
        {
            core = new MySQLCore(plugin.getSettingsManager().getHost(), plugin.getSettingsManager().getDatabase(), plugin.getSettingsManager().getUsername(), plugin.getSettingsManager().getPassword());

            if (core.checkConnection())
            {
                SimpleClans.log(Level.INFO, "MySQL Connection successful");

                if (!core.existsTable("sc_clans"))
                {
                    SimpleClans.log(Level.INFO, "Creating table: sc_clans");

                    String query = "CREATE TABLE IF NOT EXISTS `sc_clans` ( `id` bigint(20) NOT NULL auto_increment, `verified` tinyint(1) default '0', `tag` varchar(25) NOT NULL, `color_tag` varchar(25) NOT NULL, `name` varchar(100) NOT NULL, `friendly_fire` tinyint(1) default '0', `founded` bigint NOT NULL, `last_used` bigint NOT NULL, `packed_allies` text NOT NULL, `packed_rivals` text NOT NULL, `packed_bb` mediumtext NOT NULL, `cape_url` varchar(255) NOT NULL, PRIMARY KEY  (`id`), UNIQUE KEY `uq_simpleclans_1` (`tag`));";
                    core.execute(query);
                }

                if (!core.existsTable("sc_players"))
                {
                    SimpleClans.log(Level.INFO, "Creating table: sc_players");

                    String query = "CREATE TABLE IF NOT EXISTS `sc_players` ( `id` bigint(20) NOT NULL auto_increment, `name` varchar(16) NOT NULL, `leader` tinyint(1) default '0', `tag` varchar(25) NOT NULL, `friendly_fire` tinyint(1) default '0', `neutral_kills` int(11) default NULL, `rival_kills` int(11) default NULL, `civilian_kills` int(11) default NULL, `deaths` int(11) default NULL, `last_seen` bigint NOT NULL, `join_date` bigint NOT NULL, `packed_past_clans` text, PRIMARY KEY  (`id`), UNIQUE KEY `uq_sc_players_1` (`name`));";
                    core.execute(query);
                }
            }
            else
            {
                SimpleClans.log(Level.INFO, "MySQL Connection failed");
            }
        }
        else
        {
            core = new SQLiteCore(SimpleClans.getLogger(), "SimpleClans", plugin.getDataFolder().getPath());

            if (core.checkConnection())
            {
                SimpleClans.log(Level.INFO, "SQLite Connection successful");

                if (!core.existsTable("simpleclans"))
                {
                    SimpleClans.log(Level.INFO, "Creating table: sc_clans");

                    String query = "CREATE TABLE IF NOT EXISTS `sc_clans` ( `id` bigint(20), `verified` tinyint(1) default '0', `tag` varchar(25) NOT NULL, `color_tag` varchar(25) NOT NULL, `name` varchar(100) NOT NULL, `friendly_fire` tinyint(1) default '0', `founded` bigint NOT NULL, `last_used` bigint NOT NULL, `packed_allies` text NOT NULL, `packed_rivals` text NOT NULL, `packed_bb` mediumtext NOT NULL, `cape_url` varchar(255) NOT NULL, PRIMARY KEY  (`id`), UNIQUE (`tag`));";
                    core.execute(query);
                }

                if (!core.existsTable("sc_players"))
                {
                    SimpleClans.log(Level.INFO, "Creating table: sc_players");

                    String query = "CREATE TABLE IF NOT EXISTS `sc_players` ( `id` bigint(20), `name` varchar(16) NOT NULL, `leader` tinyint(1) default '0', `tag` varchar(25) NOT NULL, `friendly_fire` tinyint(1) default '0', `neutral_kills` int(11) default NULL, `rival_kills` int(11) default NULL, `civilian_kills` int(11) default NULL, `deaths` int(11) default NULL, `last_seen` bigint NOT NULL, `join_date` bigint NOT NULL, `packed_past_clans` text, PRIMARY KEY  (`id`), UNIQUE (`name`));";
                    core.execute(query);
                }
            }
            else
            {
                SimpleClans.log(Level.INFO, " SQLite Connection failed");
            }
        }
    }

    private void importFromDatabase()
    {
        plugin.getClanManager().cleanData();

        List<Clan> simpleclans = getSimpleClans();
        List<ClanPlayer> tps = getClanPlayers();

        inactivityPurge(simpleclans, tps);

        for (Clan clan : simpleclans)
        {
            plugin.getClanManager().importClan(clan);
        }

        if (simpleclans.size() > 0)
        {
            SimpleClans.log(Level.INFO, "simpleclans: {0}", simpleclans.size());
        }

        for (ClanPlayer cp : tps)
        {
            Clan tm = plugin.getClanManager().getClan(cp.getTag());

            if (tm != null)
            {
                tm.addMember(cp);
            }
            plugin.getClanManager().importClanPlayer(cp);
        }

        if (tps.size() > 0)
        {
            SimpleClans.log(Level.INFO, "clan players: {0}", tps.size());
        }
    }

    private void inactivityPurge(List<Clan> simpleclans, List<ClanPlayer> tps)
    {
        List<Clan> purgeSimpleClans = new ArrayList<Clan>();
        List<ClanPlayer> purgeTps = new ArrayList<ClanPlayer>();

        for (Clan clan : simpleclans)
        {
            if (plugin.getClanManager().isVerified(clan))
            {
                if (clan.getInactiveDays() > plugin.getSettingsManager().getPurgeClan())
                {
                    purgeSimpleClans.add(clan);
                }
            }
            else
            {
                if (clan.getInactiveDays() > plugin.getSettingsManager().getPurgeUnverified())
                {
                    purgeSimpleClans.add(clan);
                }
            }
        }

        for (ClanPlayer cp : tps)
        {
            if (cp.getInactiveDays() > plugin.getSettingsManager().getPurgePlayers())
            {
                purgeTps.add(cp);
            }
        }

        for (Clan clan : purgeSimpleClans)
        {
            SimpleClans.log(Level.INFO, "Purging clan: {0}", clan.getName());
            deleteClan(clan);
            simpleclans.remove(clan);
        }

        for (ClanPlayer cp : purgeTps)
        {
            SimpleClans.log(Level.INFO, "Purging player's data {0}", cp.getName());
            deleteClanPlayer(cp);
            tps.remove(cp);
        }
    }

    /**
     * Retrieves all simpleclans from the database
     * @return
     */
    public List<Clan> getSimpleClans()
    {
        List<Clan> out = new ArrayList<Clan>();

        String query = "SELECT * FROM  `sc_clans`;";
        ResultSet res = core.select(query);

        if (res != null)
        {
            try
            {
                while (res.next())
                {
                    try
                    {
                        boolean verified = res.getBoolean("verified");
                        boolean friendly_fire = res.getBoolean("friendly_fire");
                        String tag = res.getString("tag");
                        String color_tag = res.getString("color_tag");
                        String name = res.getString("name");
                        String packed_allies = res.getString("packed_allies");
                        String packed_rivals = res.getString("packed_rivals");
                        String packed_bb = res.getString("packed_bb");
                        String cape_url = res.getString("cape_url");
                        long founded = res.getLong("founded");
                        long last_used = res.getLong("last_used");

                        if (founded == 0)
                        {
                            founded = (new Date()).getTime();
                        }

                        if (last_used == 0)
                        {
                            last_used = (new Date()).getTime();
                        }

                        Clan clan = new Clan();
                        clan.setVerified(verified);
                        clan.setFriendlyFire(friendly_fire);
                        clan.setTag(tag);
                        clan.setColorTag(color_tag);
                        clan.setName(name);
                        clan.setPackedAllies(packed_allies);
                        clan.setPackedRivals(packed_rivals);
                        clan.setPackedBb(packed_bb);
                        clan.setCapeUrl(cape_url);
                        clan.setFounded(founded);
                        clan.setLastUsed(last_used);

                        out.add(clan);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
            catch (SQLException ex)
            {
                Logger.getLogger(StorageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return out;
    }

    /**
     * Retrieves all clan players from the database
     * @return
     */
    public List<ClanPlayer> getClanPlayers()
    {
        List<ClanPlayer> out = new ArrayList<ClanPlayer>();

        String query = "SELECT * FROM  `sc_players`;";
        ResultSet res = core.select(query);

        if (res != null)
        {
            try
            {
                while (res.next())
                {
                    try
                    {
                        String name = res.getString("name");
                        String tag = res.getString("tag");
                        boolean leader = res.getBoolean("leader");
                        boolean friendly_fire = res.getBoolean("friendly_fire");
                        int neutral_kills = res.getInt("neutral_kills");
                        int rival_kills = res.getInt("rival_kills");
                        int civilian_kills = res.getInt("civilian_kills");
                        int deaths = res.getInt("deaths");
                        long last_seen = res.getLong("last_seen");
                        long join_date = res.getLong("join_date");
                        String packed_past_clans = res.getString("packed_past_clans");

                        if (last_seen == 0)
                        {
                            last_seen = (new Date()).getTime();
                        }

                        if (join_date == 0)
                        {
                            join_date = (new Date()).getTime();
                        }

                        ClanPlayer cp = new ClanPlayer();
                        cp.setName(name);
                        cp.setTag(tag);
                        cp.setLeader(leader);
                        cp.setFriendlyFire(friendly_fire);
                        cp.setNeutralKills(neutral_kills);
                        cp.setRivalKills(rival_kills);
                        cp.setCivilianKills(civilian_kills);
                        cp.setDeaths(deaths);
                        cp.setLastSeen(last_seen);
                        cp.setJoinDate(join_date);
                        cp.setPackedPastClans(packed_past_clans);

                        out.add(cp);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
            catch (SQLException ex)
            {
                Logger.getLogger(StorageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return out;
    }

    /**
     * Insert a clan into the database
     * @param clan
     */
    public void insertClan(Clan clan)
    {
        String query = "INSERT INTO `sc_clans` (  `verified`, `tag`, `color_tag`, `name`, `friendly_fire`, `founded`, `last_used`, `packed_allies`, `packed_rivals`, `packed_bb`, `cape_url`) ";
        String values = "VALUES ( " + (clan.isVerified() ? 1 : 0) + ",'" + Helper.escapeQuotes(clan.getTag()) + "','" + Helper.escapeQuotes(clan.getColorTag()) + "','" + Helper.escapeQuotes(clan.getName()) + "'," + (clan.isFriendlyFire() ? 1 : 0) + ",'" + clan.getFounded() + "','" + clan.getLastUsed() + "','" + Helper.escapeQuotes(clan.getPackedAllies()) + "','" + Helper.escapeQuotes(clan.getPackedRivals()) + "','" + Helper.escapeQuotes(clan.getPackedBb()) + "','" + Helper.escapeQuotes(clan.getCapeUrl()) + "');";
        core.insert(query + values);
    }

    /**
     * Update a clan from the database
     * @param clan
     */
    public void updateClan(Clan clan)
    {
        clan.updateLastUsed();
        String query = "UPDATE `sc_clans` SET verified = " + (clan.isVerified() ? 1 : 0) + ", tag = '" + Helper.escapeQuotes(clan.getTag()) + "', color_tag = '" + Helper.escapeQuotes(clan.getColorTag()) + "', name = '" + Helper.escapeQuotes(clan.getName()) + "', friendly_fire = " + (clan.isFriendlyFire() ? 1 : 0) + ", founded = '" + clan.getFounded() + "', last_used = '" + clan.getLastUsed() + "', packed_allies = '" + Helper.escapeQuotes(clan.getPackedAllies()) + "', packed_rivals = '" + Helper.escapeQuotes(clan.getPackedRivals()) + "', packed_bb = '" + Helper.escapeQuotes(clan.getPackedBb()) + "', cape_url = '" + Helper.escapeQuotes(clan.getCapeUrl()) + "' WHERE tag = '" + Helper.escapeQuotes(clan.getTag()) + "';";
        core.update(query);
    }

    /**
     * Delete a clan from the database
     * @param clan
     */
    public void deleteClan(Clan clan)
    {
        String query = "DELETE FROM `sc_clans` WHERE tag = '" + clan.getTag() + "';";
        core.delete(query);
    }

    /**
     * Insert a clan player into the database
     * @param cp
     */
    public void insertClanPlayer(ClanPlayer cp)
    {
        String query = "INSERT INTO `sc_players` (  `name`, `leader`, `tag`, `friendly_fire`, `neutral_kills`, `rival_kills`, `civilian_kills`, `deaths`, `last_seen`, `join_date`, `packed_past_clans`) ";
        String values = "VALUES ( '" + cp.getName() + "'," + (cp.isLeader() ? 1 : 0) + ",'" + Helper.escapeQuotes(cp.getTag()) + "'," + (cp.isFriendlyFire() ? 1 : 0) + "," + cp.getNeutralKills() + "," + cp.getRivalKills() + "," + cp.getCivilianKills() + "," + cp.getDeaths() + ",'" + cp.getLastSeen() + "',' " + cp.getJoinDate() + "','" + Helper.escapeQuotes(cp.getPackedPastClans()) + "');";
        core.insert(query + values);
    }

    /**
     * Update a clan player from the database
     * @param cp
     */
    public void updateClanPlayer(ClanPlayer cp)
    {
        cp.updateLastSeen();
        String query = "UPDATE `sc_players` SET leader = " + (cp.isLeader() ? 1 : 0) + ", tag = '" + Helper.escapeQuotes(cp.getTag()) + "' , friendly_fire = " + (cp.isFriendlyFire() ? 1 : 0) + ", neutral_kills = " + cp.getNeutralKills() + ", rival_kills = " + cp.getRivalKills() + ", civilian_kills = " + cp.getCivilianKills() + ", deaths = " + cp.getDeaths() + ", last_seen = '" + cp.getLastSeen() + "', packed_past_clans = '" + Helper.escapeQuotes(cp.getPackedPastClans()) + "' WHERE name = '" + cp.getName() + "';";
        core.update(query);
    }

    /**
     * Delete a clan player from the database
     * @param cp
     */
    public void deleteClanPlayer(ClanPlayer cp)
    {
        String query = "DELETE FROM `sc_players` WHERE name = '" + cp.getName() + "';";
        core.delete(query);
    }
}