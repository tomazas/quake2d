
import java.util.*;

/**
 * @(#) Path.java
 */
import java.util.*;
import java.awt.*;

public class Path
{
	private vector2f          m_end;
	private vector2f          m_start;
	private ArrayList<Node>   m_nodes; 
	
	public Path(vector2f from, vector2f to)
	{
            m_start = from;
            m_end = to;
            m_nodes = new ArrayList<Node>();
	}

        // add all nodes along the path with this function
	public void appendNode(Node node){
            m_nodes.add(node);
	}
	
	public int getNumNodes( )
	{
            return m_nodes.size();
	}

        public Node getNode(int index){
            return m_nodes.get(index);
        }

        // returns next node in path after the given one
        // or Null if the list ends
        public Node getNext(Node from)
        {
            for(int i=0; i<m_nodes.size(); i++){
                if(m_nodes.get(i) != from) continue;
                if(i+1 >= m_nodes.size()) return null; // reached last node
                return m_nodes.get(i+1);
            }

            return null;
        }
	
	// returns closest node in path to a given point
	public Node getClosest( vector2f pos )
	{
            Node pick = null;
            float prevLen = -1.0f;
            
            for(int i=0; i<m_nodes.size(); i++)
            {
                vector2f delta = m_nodes.get(i).getPos().sub(pos);
                float len = delta.length();
                if(len < prevLen || prevLen < 0.0f){
                    prevLen = len;
                    pick = m_nodes.get(i);
                }
            }

            return pick;
	}
	
	public vector2f getStartPos( )
	{
            return m_start;
	}
	
	public vector2f getEndPos( )
	{
            return m_end;
	}

        // returns true if a node is in the path
        public boolean containsNode(Node node)
        {
            return m_nodes.contains(node);
        }

        public void draw(Graphics2D painter)
        {
            painter.setColor(Color.RED);

            for(int i=0; i<m_nodes.size()-1; i++){
                vector2f p0 = m_nodes.get(i).getPos();
                vector2f p1 = m_nodes.get(i+1).getPos();
                painter.drawLine((int)p0.x, (int)p0.y, (int)p1.x, (int)p1.y);
            }
            
            painter.setColor(Color.BLACK);

        }

        // smooths the current path
        // currently not used
        public void smooth(World world)  {
            int numNodes = m_nodes.size();
            if (numNodes <= 2) return;
            
            int E2num = 2;
            vector2f E1 = getNode(0).getPos();
            vector2f E2 = getNode(E2num).getPos();

            while ( !E2.equals(m_end))  {
                while ( !world.isColliding(E1, E2) )  {
                    m_nodes.remove(E2num-1);
                    E2num++;
                    numNodes--;
                    if (E2num >= numNodes)   break;
                    E2 = m_nodes.get(E2num).getPos();
                }
                E1 = E2;
                E2num+=2;
                if (E2num >= numNodes)   break;
                E2 = m_nodes.get(E2num).getPos();
            }

            /*

            int numNodes = m_nodes.size();
            if (numNodes <= 2) return;

            int startId = 0;
            vector2f E1 = m_nodes.get(startId).getPos();

            LinkedList<Node> drop_lst = new LinkedList<Node>();

            while(startId < numNodes-2){

                int nextId = startId + 2;
                vector2f E2 = m_nodes.get(nextId).getPos();

                while( !world.isColliding(E1, E2) )  {
                    drop_lst.add(m_nodes.get(nextId-1));
                    nextId++;
                    if(nextId == numNodes) break;
                    E2 = m_nodes.get(nextId).getPos();
                }

                m_nodes.removeAll(drop_lst);
                numNodes = m_nodes.size();
                startId++;
            }

            */
        }
	
}
