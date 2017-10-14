package cryodex.modules.swlcg.wizard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cryodex.CryodexController;
import cryodex.Language;
import cryodex.Player;
import cryodex.modules.Tournament;
import cryodex.widget.ComponentUtils;
import cryodex.widget.DoubleList;
import cryodex.widget.wizard.TournamentWizard;
import cryodex.widget.wizard.pages.Page;

public class PlayerSelectionPage implements Page {

        private DoubleList<Player> playerList;
        private JCheckBox removeCurrentlyPlaying;

        private JPanel pagePanel;

        @Override
        public JPanel getPanel() {

            TournamentWizard.getInstance().setButtonVisibility(true, true, null);

            TournamentWizard.getInstance().setMinimumSize(new Dimension(450, 500));

            if (pagePanel == null) {

                pagePanel = new JPanel(new BorderLayout());

                JLabel header = new JLabel("<HTML><H1>" + Language.select_players + "</H1></HTML>");

                pagePanel.add(ComponentUtils.addToFlowLayout(header, FlowLayout.CENTER), BorderLayout.NORTH);

                playerList = new DoubleList<Player>(CryodexController.getPlayers(), null, Language.available_players, Language.event_players);

                pagePanel.add(playerList, BorderLayout.CENTER);

                removeCurrentlyPlaying = new JCheckBox(Language.remove_players_currently_in_an_event);
                removeCurrentlyPlaying.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        if (removeCurrentlyPlaying.isSelected()) {
                            List<Tournament> tournaments = CryodexController.getAllTournaments();
                            List<Player> players1 = playerList.getList1Values();
                            List<Player> players2 = playerList.getList2Values();
                            for (Tournament t : tournaments) {
                                players1.removeAll(t.getPlayers());
                                players2.removeAll(t.getPlayers());
                            }
                            playerList.setValues(players1, players2);
                        } else {
                            List<Tournament> tournaments = CryodexController.getAllTournaments();
                            List<Player> players = new ArrayList<Player>();

                            for (Tournament t : tournaments) {
                                for (Player p : t.getPlayers()) {
                                    if (players.contains(p) == false) {
                                        players.add(p);
                                    }
                                }
                            }

                            List<Player> players1 = playerList.getList1Values();
                            players1.addAll(players);
                            List<Player> players2 = playerList.getList2Values();
                            playerList.setValues(players1, players2);
                        }
                    }
                });

                pagePanel.add(ComponentUtils.addToFlowLayout(removeCurrentlyPlaying, FlowLayout.CENTER), BorderLayout.SOUTH);

            }

            return pagePanel;
        }

        @Override
        public void onNext() {
            List<Player> swlcgPlayerList = new ArrayList<>();
            for (Player p : playerList.getList2Values()) {
                swlcgPlayerList.add(p);
            }
            TournamentWizard.getInstance().getWizardOptions().setPlayerList(swlcgPlayerList);
            TournamentWizard.getInstance().setCurrentPage(new AdditionalOptionsPage());
        }

        @Override
        public void onPrevious() {
            TournamentWizard.getInstance().goToPrevious();
        }

        @Override
        public void onFinish() {
            // Do Nothing
        }

    }