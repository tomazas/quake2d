/**
 * @(#) PlayState.java
 */
import java.awt.*;
import javax.swing.*;

public class PlayState extends GameState
{
	private World           m_world = null;
        private JComponent      m_canvas = null;
        private boolean         m_launched = false;
	
	public PlayState( ){ }
	
	public void onEnter( JComponent canvas )
	{
            m_canvas = canvas;
            m_world = new World();
            m_world.create(canvas); // prepare everything
	}
	
	public void onExecute( float delta )
	{
            if(m_launched){
                // simulate the world
                m_world.update( delta );

                // panel repaint, calls our PlayState::repaint from inside
                m_canvas.repaint();
            }
	}
	
	public void onExit( )
	{
            m_world.destroy();
            m_launched = false;
	}

        // Call this to repaint the elements of the game
        public void onRepaint( Graphics2D painter )
        {
            m_world.draw(painter, true);
        }

        // receive messages from the controller
        public void onMessage(Game.Message message, String params[])
        {
            try{
                switch(message){
                    case LOAD_MAP: if(m_world != null) m_world.loadMap(params[0]); break;
                    case BOT_COUNT: if(m_world != null) m_world.setBotCount( Integer.parseInt(params[0]) ); break;
                    case SIMULATE: m_launched = true; break;
                    case TOGGLE_GRAPH: m_world.toggleGraphDraw(); break;
                    case TOGGLE_PLAYER: m_world.togglePlayer(); break;
                    case TOGGLE_PATHS: m_world.togglePaths(); break;
                    case TOGGLE_RATIO: m_world.toggleRatio(); break;
                    case TOGGLE_INFO: m_world.toggleInfo(); break;
                    case MOUSE_CLICK: m_world.mouseClick(); break;
                    case RESET: m_world.resetGame(); break;
                    case KEY_EVENT: m_world.keyEvent(Integer.parseInt(params[0]), Integer.parseInt(params[1])); break;
                    case MOUSE_MOVE: m_world.updateMouse(Integer.parseInt(params[0]), Integer.parseInt(params[1])); break;
                    default: System.out.println("Unknown message: "+message); break;
                }
            }catch(NumberFormatException e){
                // do nothing
            }catch(Exception e){
                e.printStackTrace();
            }
        }
	
}
