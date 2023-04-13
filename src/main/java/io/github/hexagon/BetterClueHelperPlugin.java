package io.github.hexagon;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.dbtable.DBRowConfig;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(name = "Better Clue Helper")
public class BetterClueHelperPlugin extends Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BetterClueHelperPlugin.class);

    private static final int OBJ_PARAM_CLUE_ROW_ID = 623;

    private static final int ANAGRAM_TABLE = 4;
    private static final int ANAGRAM_COLUMN_DIFFICULTY = 1;
    private static final int ANAGRAM_COLUMN_TEXT = 2;
    private static final int ANAGRAM_COLUMN_SOLUTION = 3;
    private static final int ANAGRAM_COLUMN_CHALLENGE_ANSWER = 4;
    private static final int ANAGRAM_COLUMN_ACCESS_RESTRICTION = 4;

    private static final int TARGET_NPC_TABLE = 15;
    private static final int TARGET_NPC_COLUMN_NPC = 0;

    private static final int TARGET_LOC_TABLE = 16;
    private static final int TARGET_COORD_GRID_TABLE = 18;

    /*
     * enum_4616 = clue difficulty
     *
     * [dbtable_4] // anagram clues
     * column=dbcolumn_0,int,REQUIRED,INDEXED,CLIENTSIDE
     * column=dbcolumn_1,int,REQUIRED,CLIENTSIDE // clue difficulty
     * column=dbcolumn_2,string,REQUIRED,CLIENTSIDE // the anagram
     * column=dbcolumn_3,dbrow,REQUIRED,LIST,CLIENTSIDE // the solution
     * column=dbcolumn_4,dbrow,CLIENTSIDE // challenge answer
     * column=dbcolumn_5,dbrow,CLIENTSIDE // access restrictions (Ex: Access to Lletya required.)
     *
     * [dbtable_15] // npcs (and their locations)
     * column=dbcolumn_0,npc,REQUIRED,CLIENTSIDE
     * column=dbcolumn_1,npc,CLIENTSIDE // used for multinpcs
     * column=dbcolumn_2,coord,REQUIRED,CLIENTSIDE
     * column=dbcolumn_3,string,REQUIRED,CLIENTSIDE // the location description
     *
     * [dbtable_16] // locs (and their locations)
     * column=dbcolumn_0,loc,REQUIRED,CLIENTSIDE
     * column=dbcolumn_1,loc,CLIENTSIDE // used for multilocs
     * column=dbcolumn_2,coord,REQUIRED,CLIENTSIDE
     * column=dbcolumn_3,string,REQUIRED,CLIENTSIDE // the location description
     *
     * [dbtable_18] // dig at solutions
     * column=dbcolumn_0,coord,REQUIRED,CLIENTSIDE
     * column=dbcolumn_1,string,REQUIRED,CLIENTSIDE // the dig location description
     * column=dbcolumn_2,string,CLIENTSIDE // the dig location short description
     *
     * [dbtable_19] // med/elite clue key challenges
     * column=dbcolumn_0,loc,REQUIRED,CLIENTSIDE // the loc that needs to be opened
     * column=dbcolumn_1,coord,REQUIRED,CLIENTSIDE // the location of the loc
     * column=dbcolumn_2,npc,REQUIRED,CLIENTSIDE // npc to be killed for the key (tuple)
     * column=dbcolumn_3,namedobj,REQUIRED,CLIENTSIDE // the key that needs to be obtained
     * column=dbcolumn_4,coord,REQUIRED,CLIENTSIDE // where to obtain the key
     * column=dbcolumn_5,inv,CLIENTSIDE
     * column=dbcolumn_6,int,CLIENTSIDE
     * column=dbcolumn_7,string,REQUIRED,CLIENTSIDE // solution help text
     * default=dbcolumn_5,inv
     * default=dbcolumn_6,1
     *
     * [dbtable_20] // kill clues
     * column=dbcolumn_0,npc,REQUIRED,CLIENTSIDE // the npc to be killed
     * column=dbcolumn_1,coord,CLIENTSIDE
     * column=dbcolumn_2,string,REQUIRED,CLIENTSIDE // The description (kill a hellhound.)
     *
     * [dbtable_21] // falo the bard clues
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // the obj name
     * column=dbcolumn_1,namedobj,REQUIRED,CLIENTSIDE // the obj
     * column=dbcolumn_2,inv,CLIENTSIDE
     * column=dbcolumn_3,int,CLIENTSIDE
     * default=dbcolumn_2,inv
     * default=dbcolumn_3,1
     *
     * [dbtable_22] // generic obj references
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // the generic obj reference (any dragon scimitar)
     * column=dbcolumn_1,int,REQUIRED,CLIENTSIDE
     *
     * [dbtable_23] // quest requiriments (currently there's only one row)
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // The requirement (Access to Lletya required.)
     * column=dbcolumn_1,dbrow,REQUIRED,CLIENTSIDE // references a quest
     * column=dbcolumn_2,int,REQUIRED,CLIENTSIDE // likely the required quest stage
     *
     * [dbtable_24] // skill requirements
     * column=dbcolumn_0,stat,REQUIRED,CLIENTSIDE
     * column=dbcolumn_1,int,REQUIRED,CLIENTSIDE // level required
     *
     * [dbtable_25] // simple challenges, solution is split by ,
     * column=dbcolumn_0,string,int,REQUIRED,CLIENTSIDE // If x is 15 and y is 3 what is 3x + y?,48 (tuple)
     *
     *
     * [dbtable_26] // puzzle type
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // A puzzle or a light box.

     * [dbtable_27] // clue guardians
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // description
     * column=dbcolumn_1,npc,REQUIRED,CLIENTSIDE // (tuple)

     * [dbtable_28] // emote clues
     * column=dbcolumn_0,string,REQUIRED,CLIENTSIDE // short description
     * column=dbcolumn_1,namedobj,CLIENTSIDE // which obj should be worn at slot 1, 6512 means nothing
     * column=dbcolumn_2,namedobj,CLIENTSIDE
     * column=dbcolumn_3,namedobj,CLIENTSIDE
     * column=dbcolumn_4,namedobj,CLIENTSIDE
     * column=dbcolumn_5,namedobj,CLIENTSIDE
     * column=dbcolumn_6,namedobj,CLIENTSIDE
     * column=dbcolumn_7,namedobj,CLIENTSIDE
     * column=dbcolumn_8,namedobj,CLIENTSIDE
     * column=dbcolumn_9,namedobj,CLIENTSIDE
     * column=dbcolumn_10,namedobj,CLIENTSIDE
     * column=dbcolumn_11,namedobj,CLIENTSIDE
     * column=dbcolumn_12,int,CLIENTSIDE // the item 'category' (param 258) that should be worn at slot 1
     * column=dbcolumn_13,int,CLIENTSIDE
     * column=dbcolumn_14,int,CLIENTSIDE
     * column=dbcolumn_15,int,CLIENTSIDE
     * column=dbcolumn_16,int,CLIENTSIDE
     * column=dbcolumn_17,int,CLIENTSIDE
     * column=dbcolumn_18,int,CLIENTSIDE
     * column=dbcolumn_19,int,CLIENTSIDE
     * column=dbcolumn_20,int,CLIENTSIDE
     * column=dbcolumn_21,int,CLIENTSIDE
     * column=dbcolumn_22,int,CLIENTSIDE
     * column=dbcolumn_23,int,CLIENTSIDE
     */

    // we need  db_getfieldcount = 7503 support for tuples

    @Inject
    private Client client;

    private ClueSolution clueSolutionForObj(int objId) {
        ItemComposition itemDefinition = client.getItemDefinition(objId);
        int clueRowId = itemDefinition.getIntValue(OBJ_PARAM_CLUE_ROW_ID);
        DBRowConfig dbRowConfig = client.getDBRowConfig(clueRowId);

        if (dbRowConfig == null) {
            return null;
        }

        return switch (dbRowConfig.getTableID()) {
            case ANAGRAM_TABLE -> loadAnagramClueSolution(clueRowId);
            default -> null;
        };
    }

    private AnagramClueSolution loadAnagramClueSolution(int clueRowId) {
        var difficulty = (Integer) client.getDBTableField(ANAGRAM_TABLE, ANAGRAM_COLUMN_DIFFICULTY, 0, 0);
        var text = (String) client.getDBTableField(ANAGRAM_TABLE, ANAGRAM_COLUMN_TEXT, 0, 0);
        var target = loadTargetFromRow((Integer) client.getDBTableField(ANAGRAM_TABLE, ANAGRAM_COLUMN_SOLUTION, 0, 0));
        var challengeAnswer = client.getDBTableField(ANAGRAM_TABLE, ANAGRAM_COLUMN_CHALLENGE_ANSWER, 0, 0);

        return null;
    }

    private Target loadTargetFromRow(int tableRowId) {
        var tableId = client.getDBRowConfig(tableRowId).getTableID();
        return switch (tableId) {
            case TARGET_NPC_TABLE -> loadNpcTarget(tableRowId);
            case TARGET_LOC_TABLE -> loadLocTarget(tableRowId);
            case TARGET_COORD_GRID_TABLE -> loadCoordGridTarget(tableRowId);
            default -> null;
        };
    }

    private Target loadNpcTarget(int tableRowId) {
        return null;
    }

    private Target loadLocTarget(int tableRowId) {
        return null;
    }

    private Target loadCoordGridTarget(int tableRowId) {
        return null;
    }

    private enum ClueType {
        ANAGRAM(ANAGRAM_TABLE),
        MAP(5),
        CIPHER(6),
        COORDINATE(7),
        CRYPTIC(8),
        EMOTE(9),
        FAIRY_RING(10),
        FALO_THE_BARD(11),
        HOT_AND_COLD(12),
        MUSIC(13),
        SKILL_CHALLENGE(14);

        private static final Map<Integer, ClueType> byTableId = new HashMap<>();

        static {
            for (ClueType clueType : values()) {
                byTableId.put(clueType.clueTableId, clueType);
            }
        }

        private final int clueTableId;

        ClueType(int clueTableId) {
            this.clueTableId = clueTableId;
        }

        public static ClueType from(int clueTableId) {
            return byTableId.get(clueTableId);
        }

        public int clueTableId() {
            return clueTableId;
        }
    }
}
