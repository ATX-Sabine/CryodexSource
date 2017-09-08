package cryodex.modules.xwing;

import cryodex.CryodexController;
import cryodex.CryodexController.Modules;
import cryodex.Main;
import cryodex.Player;
import cryodex.modules.Module;
import cryodex.modules.Tournament;
import cryodex.xml.XMLObject;
import cryodex.xml.XMLUtils;
import cryodex.xml.XMLUtils.Element;

import javax.swing.*;
import java.net.URL;
import java.util.*;

public class XWingTournament implements XMLObject, Tournament {

    public enum InitialSeedingEnum {
        RANDOM, BY_GROUP, IN_ORDER
    }

    private final List<XWingRound> rounds;
    private List<XWingPlayer> players;
    private InitialSeedingEnum seedingEnum;
    private final XWingTournamentGUI tournamentGUI;
    private String name;
    private List<Integer> points;
    private boolean startAsSingleElimination = false;
    private boolean startAsRoundRobin = false;
    private List<String> dependentTournaments;

    public XWingTournament(Element tournamentElement) {

        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.dependentTournaments = new ArrayList<>();

        seedingEnum = InitialSeedingEnum.RANDOM;

        tournamentGUI = new XWingTournamentGUI(this);

        String playerIDs = tournamentElement.getStringFromChild("PLAYERS");

        Module m = Modules.getModuleByName(getModuleName());

        for (String s : playerIDs.split(",")) {
            Player p = CryodexController.getPlayerByID(s);

            if (p != null) {
                XWingPlayer xp = (XWingPlayer) p.getModuleInfoByModule(m);
                if (xp != null) {
                    players.add(xp);
                }
            }
        }

        Element roundElement = tournamentElement.getChild("ROUNDS");
        for (Element e : roundElement.getChildren()) {
            rounds.add(new XWingRound(e, this));

        }

        Element dependentElement = tournamentElement.getChild("DEPENDENTTOURNAMENTS");
        if (dependentElement != null) {
            for (Element e : dependentElement.getChildren()) {
                dependentTournaments.add(e.getData());
            }
        }

        name = tournamentElement.getStringFromChild("NAME");
        String seeding = tournamentElement.getStringFromChild("SEEDING");
        if (seeding != null && seeding.isEmpty() == false) {
            seedingEnum = InitialSeedingEnum.valueOf(seeding);
        } else {
            seedingEnum = InitialSeedingEnum.RANDOM;
        }

        String pointsString = tournamentElement.getStringFromChild("POINTS");

        if (pointsString != null && pointsString.isEmpty() == false) {
            points = new ArrayList<Integer>();
            for (String s : pointsString.split(",")) {
                points.add(new Integer(s));
            }
        }

        int counter = 1;
        for (XWingRound r : rounds) {
            if (r.isSingleElimination()) {
                getTournamentGUI().getRoundTabbedPane().addSingleEliminationTab(r.getMatches().size() * 2, r.getPanel());
            } else {
                if (r.isRoundRobin()) {
                    this.startAsRoundRobin = true;
                }
                getTournamentGUI().getRoundTabbedPane().addSwissTab(counter, r.getPanel());
                counter++;
            }

        }

        getTournamentGUI().getRankingTable().setPlayers(getAllXWingPlayers());
    }

    public XWingTournament(String name, List<XWingPlayer> players, InitialSeedingEnum seedingEnum, List<Integer> points, boolean isSingleElimination,
                           boolean isRoundRobin) {
        this.name = name;
        this.players = new ArrayList<>(players);
        this.rounds = new ArrayList<>();
        this.seedingEnum = seedingEnum;
        this.points = points;
        this.startAsSingleElimination = isSingleElimination;
        this.startAsRoundRobin = isRoundRobin;
        this.dependentTournaments = new ArrayList<String>();

        tournamentGUI = new XWingTournamentGUI(this);
    }

    public XWingRound getLatestRound() {
        if (rounds == null || rounds.isEmpty()) {
            return null;
        } else {
            return rounds.get(rounds.size() - 1);
        }
    }

    public int getRoundNumber(XWingRound round) {
        int count = 0;
        for (XWingRound r : rounds) {
            count++;
            if (r == round) {
                return count;
            }
        }

        return 0;
    }

    public XWingRound getRound(int i) {
        if (rounds == null) {
            return null;
        } else {
            return rounds.get(i);
        }
    }

    public XWingRound getSelectedRound() {
        if (rounds == null) {
            return null;
        } else {
            return getAllRounds().get(getTournamentGUI().getRoundTabbedPane().getSelectedIndex());
        }
    }

    public List<XWingRound> getAllRounds() {
        return rounds;
    }

    @Override
    public int getRoundCount() {
        if (rounds == null) {
            return 0;
        } else {
            return rounds.size();
        }
    }

    @Override
    public void setPlayers(List<Player> players) {
        List<XWingPlayer> xwPlayers = new ArrayList<>();

        for (Player p : players) {
            XWingPlayer xp = new XWingPlayer(p);
            xwPlayers.add(xp);
        }

        setXWingPlayer(xwPlayers);
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<Player>();

        for (XWingPlayer xp : getXWingPlayers()) {
            players.add(xp.getPlayer());
        }

        return players;
    }

    public List<XWingPlayer> getXWingPlayers() {
        return players;
    }

    public void setXWingPlayer(List<XWingPlayer> players) {
        this.players = players;
    }

    /**
     * Returns any players have have played at least one match. This calls back dropped players into the list.
     *
     * @return
     */
    public Set<XWingPlayer> getAllXWingPlayers() {
        // TreeSets and Head To Head comparisons can have problems.
        // Do not use them together.
        Set<XWingPlayer> allPlayers = new TreeSet<XWingPlayer>(new XWingComparator(this, XWingComparator.rankingCompareNoHeadToHead));

        for (XWingRound r : getAllRounds()) {
            for (XWingMatch m : r.getMatches()) {
                if (m.isBye()) {
                    allPlayers.add(m.getPlayer1());
                } else {
                    allPlayers.add(m.getPlayer1());
                    if (m.getPlayer2() != null) {
                        allPlayers.add(m.getPlayer2());
                    }
                }
            }
        }

        allPlayers.addAll(players);

        return allPlayers;
    }

    @Override
    public Set<Player> getAllPlayers() {
        Set<Player> players = new TreeSet<Player>();

        for (XWingPlayer xp : getAllXWingPlayers()) {
            players.add(xp.getPlayer());
        }

        return players;
    }

    @Override
    public XWingTournamentGUI getTournamentGUI() {
        return tournamentGUI;
    }

    @Override
    public String getName() {
        return name;
    }

    public List<Integer> getPoints() {
        return points;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void updateVisualOptions() {
        if (CryodexController.isLoading == false) {
            for (XWingRound r : getAllRounds()) {
                r.getPanel().resetGamePanels(true);
            }
        }
    }

    @Override
    public boolean generateNextRound() {

        // All matches must have a result filled in
        if (getLatestRound().isComplete() == false) {
            JOptionPane.showMessageDialog(Main.getInstance(), "Current round is not complete. Please complete all matches before continuing");
            return false;
        }

        // Single elimination checks
        if (getLatestRound().isSingleElimination()) {
            // If there was only one match then there is no reason to create a
            // new round.
            if (getLatestRound().getMatches().size() == 1) {
                JOptionPane.showMessageDialog(Main.getInstance(), "Final tournament complete. No more rounds will be generated.");
                return false;
            }

            if (getLatestRound().isValid() == false) {
                JOptionPane.showMessageDialog(Main.getInstance(),
                        "At least one tournamnt result is not correct.\n" + "-Check if points are backwards or a draw has been set.\n"
                                + "-Draws are not allowed in single elimination rounds.\n" + "--If a draw occurs, the player with initiative wins.\n"
                                + "--This can be set by going to the X-Wing menu then the view submenu and deselect 'Enter Only Points'");
                return false;
            }

            generateSingleEliminationMatches(getLatestRound().getMatches().size());
        } else {
            // Regular swiss round checks
            if (getLatestRound().isValid() == false) {
                JOptionPane.showMessageDialog(Main.getInstance(),
                        "At least one tournamnt result is not correct. Check if points are backwards or a result should be a modified win or tie.");
                return false;
            }
            if (startAsRoundRobin) {
                if ((players.size() % 2 == 1 && rounds.size() >= players.size())
                        || (players.size() % 2 == 0 && rounds.size() >= players.size() - 1)) {
                    JOptionPane.showMessageDialog(Main.getInstance(), "Final tournament complete. No more rounds will be generated.");
                    return false;
                }
            }

            generateRound(getAllRounds().size() + 1);
        }
        return true;
    }

    @Override
    public void cancelRound(int roundNumber) {
        if (rounds.size() >= roundNumber) {
            // If we are generating a past round. Clear all existing rounds that
            // will be erased.
            while (rounds.size() >= roundNumber) {
                int index = rounds.size() - 1;
                XWingRound roundToRemove = rounds.get(index);
                for (XWingMatch m : roundToRemove.getMatches()) {
                    m.setWinner(null);
                    m.setBye(false);
                    m.setPlayer1(null);
                    m.setPlayer2(null);
                    m.setPlayer1PointsDestroyed(null);
                    m.setPlayer2PointsDestroyed(null);
                }
                rounds.remove(roundToRemove);

                getTournamentGUI().getRoundTabbedPane().remove(index);
            }
        }
    }

    private void generateRoundRobinRound(int roundNumber) {
        List<XWingMatch> matches;

        matches = new ArrayList<XWingMatch>();
        List<XWingPlayer> tempList = new ArrayList<>();
        tempList.addAll(getXWingPlayers());

        // "Rotate" some or all of the players to
        // make the matchups
        XWingPlayer tempPlayer;

        // if the number of players are odd, rotate the whole
        // list of players
        // if the number of players are even, rotate all
        // but one player
        int rotationPlace = 0;
        if (tempList.size() % 2 == 0) {
            rotationPlace++;
        }

        // once for each round after the first,
        // move one player to the back of the list
        // Either the first in the list (for an odd nr of players)
        // Or the second in the list (for an even nr of players)
        for (int n = 1; n < roundNumber; n++) {
            tempPlayer = tempList.remove(rotationPlace);
            tempList.add(tempPlayer);
        }

        matches = initialSeedingInOrder(tempList);

        XWingRound r = new XWingRound(matches, this, roundNumber);
        r.setRoundRobin(true);
        rounds.add(r);
        {
            getTournamentGUI().getRoundTabbedPane().addSwissTab(roundNumber, r.getPanel());
        }

        getTournamentGUI().getRankingTable().setPlayers(getAllXWingPlayers());

    }

    @Override
    public void generateRound(int roundNumber) {

        // if trying to skip a round...stop it
        if (roundNumber > rounds.size() + 1) {
            throw new IllegalArgumentException();
        }

        cancelRound(roundNumber);
        if (startAsRoundRobin) {
            generateRoundRobinRound(roundNumber);
            return;
        }

        List<XWingMatch> matches;

        boolean hasDependentTournaments = dependentTournaments != null && !dependentTournaments.isEmpty();

        if (roundNumber == 1 && !hasDependentTournaments) {
            matches = firstRoundPairings();
        } else {
            matches = getMatches(getXWingPlayers());
        }

        XWingRound r = new XWingRound(matches, this, roundNumber);
        rounds.add(r);
        if (roundNumber == 1 && startAsSingleElimination && (matches.size() == 1 || matches.size() == 2 || matches.size() == 4 || matches.size() == 8
                || matches.size() == 16 || matches.size() == 32)) {
            r.setSingleElimination(true);
            getTournamentGUI().getRoundTabbedPane().addSingleEliminationTab(r.getMatches().size() * 2, r.getPanel());
        } else {
            getTournamentGUI().getRoundTabbedPane().addSwissTab(roundNumber, r.getPanel());
        }

        getTournamentGUI().getRankingTable().setPlayers(getAllXWingPlayers());
    }

    private List<XWingMatch> firstRoundPairings() {
        List<XWingMatch> matches;
        List<XWingPlayer> nonPairedPlayers = new ArrayList<>();
        nonPairedPlayers.addAll(getXWingPlayers());

        List<XWingPlayer> firstRoundByePlayers = new ArrayList<>();
        for (XWingPlayer p : nonPairedPlayers) {
            if (p.isFirstRoundBye()) {
                firstRoundByePlayers.add(p);
            }
        }
        nonPairedPlayers.removeAll(firstRoundByePlayers);

        switch(seedingEnum) {
            case IN_ORDER:
                matches = initialSeedingInOrder(nonPairedPlayers);
                break;
            case BY_GROUP:
                matches = initialSeedingByGroup(nonPairedPlayers);
                break;
            default:
                matches = initialSeedingRandom(nonPairedPlayers);
                break;
        }

        for (XWingPlayer p : firstRoundByePlayers) {
            matches.add(new XWingMatch(p, null));
        }
        return matches;
    }

    private List<XWingMatch> initialSeedingByGroup(List<XWingPlayer> players) {
        List<XWingMatch> matches = new ArrayList<>();
        Map<String, List<XWingPlayer>> playerMap = new HashMap<>();

        // Add players to map
        for (XWingPlayer p : players) {
            List<XWingPlayer> playerList = playerMap.get(p.getPlayer().getGroupName());
            if (playerList == null) {
                playerList = new ArrayList<>();
                String groupName = p.getPlayer().getGroupName();
                playerMap.put(groupName, playerList);
            }
            playerList.add(p);
        }

        // Shuffle up the lists
        List<String> seedValues = new ArrayList<>(playerMap.keySet());
        Collections.shuffle(seedValues);

        // Shuffle each group list
        for (List<XWingPlayer> list : playerMap.values()) {
            Collections.shuffle(list);
        }

        XWingPlayer p1;
        XWingPlayer p2 = null;
        while (!playerMap.isEmpty()) {
            String biggestGroup = "";
            int playersInGroup = -1;

            //find biggest group
            for (Map.Entry<String, List<XWingPlayer>> entry : playerMap.entrySet()) {
                if (entry.getValue().size() > playersInGroup || biggestGroup.equals("")) {
                    biggestGroup = entry.getKey();
                    playersInGroup = entry.getValue().size();
                }
            }

            //ready player one
            p1 = playerMap.get(biggestGroup).get(0);
            playerMap.get(biggestGroup).remove(p1);
            if (playerMap.get(biggestGroup).isEmpty()) {
                playerMap.remove(biggestGroup);
            }

            //ready player two
            List<String> keys = new ArrayList<>(playerMap.keySet());
            Collections.shuffle(keys);
            for (String key : keys) {
                if (biggestGroup.equals("") || (!biggestGroup.equals(key))) {
                    p2 = playerMap.get(key).get(0);
                }
            }

            //can't find a player 2, take one from own team
            if (p2 == null) {
                for (Map.Entry<String, List<XWingPlayer>> entry : playerMap.entrySet()) {
                    p2 = entry.getValue().get(0);
                }
            }

            if (p2 != null) {
                String groupName = p2.getPlayer().getGroupName();
                playerMap.get(groupName).remove(p2);
                if (playerMap.get(groupName).isEmpty()) {
                    playerMap.remove(groupName);
                }
            }

            matches.add(new XWingMatch(p1, p2));
            p2 = null;
        }
        return matches;
    }

    private List<XWingMatch> initialSeedingRandom(List<XWingPlayer> players) {
        List<XWingMatch> matches = new ArrayList<>();
        Collections.shuffle(players);

        while (!players.isEmpty()) {
            XWingPlayer player1 = players.get(0);
            XWingPlayer player2 = players.get(players.size() - 1);
            players.remove(player1);
            if (player1 == player2) {
                player2 = null;
            } else {
                players.remove(player2);
            }

            XWingMatch match = new XWingMatch(player1, player2);
            matches.add(match);
        }
        return matches;
    }

    private List<XWingMatch> initialSeedingInOrder(List<XWingPlayer> players) {
        List<XWingMatch> matches = new ArrayList<>();
        while (!players.isEmpty()) {
            XWingPlayer player1 = players.get(0);
            XWingPlayer player2 = null;
            players.remove(0);
            if (!players.isEmpty()) {
                player2 = players.get(0);
                players.remove(0);
            }

            XWingMatch match = new XWingMatch(player1, player2);
            matches.add(match);
        }
        return matches;
    }

    private List<XWingMatch> getMatches(List<XWingPlayer> userList) {
        List<XWingMatch> matches = new ArrayList<XWingMatch>();

        List<XWingPlayer> tempList = new ArrayList<XWingPlayer>();
        tempList.addAll(userList);
        Collections.sort(tempList, new XWingComparator(this, XWingComparator.pairingCompare));

        XWingMatch byeMatch = null;
        // Setup the bye match if necessary
        // The player to get the bye is the lowest ranked player who has not had
        // a bye yet or who has the fewest byes
        if (tempList.size() % 2 == 1) {
            XWingPlayer byeUser = null;
            int byUserCounter = 1;
            int minByes = 0;
            try {
                while (byeUser == null || byeUser.getByes(this) > minByes
                        || (byeUser.getMatches(this) != null && byeUser.getMatches(this).get(byeUser.getMatches(this).size() - 1).isBye())) {
                    if (byUserCounter > tempList.size()) {
                        minByes++;
                        byUserCounter = 1;
                    }
                    byeUser = tempList.get(tempList.size() - byUserCounter);

                    byUserCounter++;

                }
            } catch (ArrayIndexOutOfBoundsException e) {
                byeUser = tempList.get(tempList.size() - 1);
            }
            byeMatch = new XWingMatch(byeUser, null);
            tempList.remove(byeUser);
        }

        matches = new XWingRandomMatchGeneration(this, tempList).generateMatches();

        if (XWingMatch.hasDuplicate(matches)) {
            JOptionPane.showMessageDialog(Main.getInstance(), "Unable to resolve duplicate matches. Please review for best course of action.");
        }

        // Add the bye match at the end
        if (byeMatch != null) {
            matches.add(byeMatch);
        }

        return matches;
    }

    @Override
    public void generateSingleEliminationMatches(int cutSize) {

        List<XWingMatch> matches = new ArrayList<>();

        List<XWingMatch> matchesCorrected = new ArrayList<XWingMatch>();

        if (getLatestRound().isSingleElimination()) {
            List<XWingMatch> lastRoundMatches = getLatestRound().getMatches();

            for (int index = 0; index < lastRoundMatches.size(); index = index + 2) {
                XWingMatch newMatch = new XWingMatch(lastRoundMatches.get(index).getWinner(), lastRoundMatches.get(index + 1).getWinner());
                matches.add(newMatch);
            }

            matchesCorrected = matches;
        } else {
            List<XWingPlayer> tempList = new ArrayList<>();
            tempList.addAll(getXWingPlayers());
            Collections.sort(tempList, new XWingComparator(this, XWingComparator.rankingCompare));
            tempList = tempList.subList(0, cutSize);

            while (tempList.isEmpty() == false) {
                XWingPlayer player1 = tempList.get(0);
                XWingPlayer player2 = tempList.get(tempList.size() - 1);
                tempList.remove(player1);
                if (player1 == player2) {
                    player2 = null;
                } else {
                    tempList.remove(player2);
                }

                XWingMatch match = new XWingMatch(player1, player2);
                matches.add(match);
            }

            switch (matches.size()) {
                case 4:
                    matchesCorrected.add(matches.get(0));
                    matchesCorrected.add(matches.get(3));
                    matchesCorrected.add(matches.get(2));
                    matchesCorrected.add(matches.get(1));
                    break;
                case 8:
                    matchesCorrected.add(matches.get(0));
                    matchesCorrected.add(matches.get(7));
                    matchesCorrected.add(matches.get(4));
                    matchesCorrected.add(matches.get(3));
                    matchesCorrected.add(matches.get(2));
                    matchesCorrected.add(matches.get(5));
                    matchesCorrected.add(matches.get(6));
                    matchesCorrected.add(matches.get(1));
                    break;
                default:
                    matchesCorrected = matches;
            }
        }

        XWingRound r = new XWingRound(matchesCorrected, this, null);
        r.setSingleElimination(true);
        rounds.add(r);
        getTournamentGUI().getRoundTabbedPane().addSingleEliminationTab(cutSize, r.getPanel());

        CryodexController.saveData();
    }

    @Override
    public StringBuilder appendXML(StringBuilder sb) {

        String playerString = "";
        String seperator = "";
        for (XWingPlayer p : players) {
            playerString += seperator + p.getPlayer().getSaveId();
            seperator = ",";
        }

        XMLUtils.appendObject(sb, "PLAYERS", playerString);

        XMLUtils.appendList(sb, "ROUNDS", "ROUND", getAllRounds());

        String pointsString = "";
        seperator = "";
        if (points != null) {
            for (Integer p : points) {
                pointsString += seperator + p;
                seperator = ",";
            }
        }

        XMLUtils.appendObject(sb, "POINTS", pointsString);
        XMLUtils.appendObject(sb, "NAME", name);
        XMLUtils.appendObject(sb, "MODULE", Modules.XWING.getName());
        XMLUtils.appendObject(sb, "SEEDING", seedingEnum);
        XMLUtils.appendStringList(sb, "DEPENDENTTOURNAMENTS", "DEPENDENT", dependentTournaments);

        return sb;
    }

    @Override
    public void startTournament() {
        generateRound(1);
    }

    @Override
    public void addPlayer(Player p) {

        for (XWingRound r : getAllRounds()) {
            for (XWingMatch m : r.getMatches()) {
                if (m.getPlayer1().getPlayer().equals(p)) {
                    getXWingPlayers().add(m.getPlayer1());
                    return;
                } else if (m.getPlayer2() != null && m.getPlayer2().getPlayer().equals(p)) {
                    getXWingPlayers().add(m.getPlayer2());
                    return;
                }
            }
        }

        XWingPlayer xPlayer = new XWingPlayer(p);
        getXWingPlayers().add(xPlayer);
    }

    @Override
    public void dropPlayer(Player p) {

        XWingPlayer xPlayer = null;

        for (XWingPlayer xp : getXWingPlayers()) {
            if (xp.getPlayer() == p) {
                xPlayer = xp;
                break;
            }
        }

        if (xPlayer != null) {
            getXWingPlayers().remove(xPlayer);
        }

        resetRankingTable();
    }

    @Override
    public void massDropPlayers(int minScore, int maxCount) {

        List<XWingPlayer> playerList = new ArrayList<XWingPlayer>();
        playerList.addAll(getXWingPlayers());

        Collections.sort(playerList, new XWingComparator(this, XWingComparator.rankingCompare));

        int count = 0;
        for (XWingPlayer xp : playerList) {
            if (xp.getScore(this) < minScore || count >= maxCount) {
                getXWingPlayers().remove(xp);
            } else {
                count++;
            }
        }

        resetRankingTable();
    }

    @Override
    public void massDropPlayers(List<Player> playersToDrop) {

        for (Player p : playersToDrop) {
            dropPlayer(p);
        }

        resetRankingTable();
    }

    @Override
    public void resetRankingTable() {
        getTournamentGUI().getRankingTable().setPlayers(getAllXWingPlayers());
    }

    @Override
    public Icon getIcon() {
        URL imgURL = XWingTournament.class.getResource("x.png");
        if (imgURL == null) {
            System.out.println("Failed to retrieve X-Wing Icon");
        }
        ImageIcon icon = new ImageIcon(imgURL);
        return icon;
    }

    @Override
    public String getModuleName() {
        return Modules.XWING.getName();
    }

    public int getRoundPoints(int roundNumber) {

        int tournamentPoints = 100;

        if (getPoints() != null && getPoints().isEmpty() == false) {

            try {
                tournamentPoints = getPoints().size() >= roundNumber ? getPoints().get(roundNumber - 1) : getPoints().get(getPoints().size() - 1);
            } catch (Exception e) {
                System.out.println("An error happened, just go with 100.");
            }
        }

        return tournamentPoints;
    }

    public void addDependentTournaments(List<XWingTournament> tournaments) {
        for (XWingTournament t : tournaments) {
            dependentTournaments.add(t.getName());
        }
    }

    public List<XWingTournament> getDependentTournaments() {

        if (dependentTournaments == null || dependentTournaments.isEmpty()) {
            return new ArrayList<XWingTournament>();
        }

        List<XWingTournament> dependentList = new ArrayList<XWingTournament>();

        for (Tournament t : CryodexController.getAllTournaments()) {
            if (t instanceof XWingTournament && dependentTournaments.contains(t.getName())) {
                dependentList.add((XWingTournament) t);
            }
        }

        return dependentList;
    }

    public void triggerChange() {
        getTournamentGUI().getRankingTable().resetPlayers();
        CryodexController.saveData();
    }

}
