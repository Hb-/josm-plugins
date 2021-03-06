// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.selection;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Unselects all nodes
 */
public class SelectModWaysAction extends JosmAction {
    private int lastHash;
    private Command lastCmd;

    public SelectModWaysAction() {
        super(tr("Select last modified ways"), "selmodways",
                tr("Select last modified ways"),
                Shortcut.registerShortcut("tools:selmodways", tr("Tool: {0}", "Select last modified ways"),
                        KeyEvent.VK_Z, Shortcut.ALT_SHIFT), true);
        putValue("help", ht("/Action/SelectLastModifiedWays"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds != null) {
            Collection<OsmPrimitive> selection = ds.getSelected();
            ds.clearSelection(OsmPrimitive.getFilteredSet(selection, Node.class));
            Command cmd;

            if (MainApplication.undoRedo.commands == null) return;
            int num = MainApplication.undoRedo.commands.size();
            if (num == 0) return;
            int k = 0, idx;
            if (selection != null && !selection.isEmpty() && selection.hashCode() == lastHash) {
                // we are selecting next command in history if nothing is selected
                idx = MainApplication.undoRedo.commands.indexOf(lastCmd);
            } else {
                idx = num;
            }

            Set<Way> ways = new HashSet<>(10);
            do {  //  select next history element
                if (idx > 0) idx--; else idx = num-1;
                cmd = MainApplication.undoRedo.commands.get(idx);
                Collection<? extends OsmPrimitive> pp = cmd.getParticipatingPrimitives();
                ways.clear();
                for (OsmPrimitive p : pp) {  // find all affected ways
                    if (p instanceof Way && !p.isDeleted()) ways.add((Way) p);
                }
                if (!ways.isEmpty() && !ds.getSelected().containsAll(ways)) {
                    ds.setSelected(ways);
                    lastCmd = cmd; // remember last used command and last selection
                    lastHash = ds.getSelected().hashCode();
                    return;
                }
                k++;
            } while (k < num); // try to find previous command if this affects nothing
            lastCmd = null;
            lastHash = 0;
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
    }
}
