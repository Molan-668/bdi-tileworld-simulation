package multiagent_tileworld_plus;

import java.awt.Color;
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

public class AgentStyle extends DefaultStyleOGL2D {

    @Override
    public Color getColor(Object o) {
        Agent agent = (Agent) o;
        return agent.hasTile() ? Color.RED : Color.BLUE;
    }
}