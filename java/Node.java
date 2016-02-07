/**
 * @(#) Node.java
 */
import java.util.*;

public class Node
{
	private vector2f        m_pos;
        private int             m_id = -1; // unique index
        
        // node can be attached to many other nodes, creating many links
	private ArrayList<Link> m_links = new ArrayList<Link>();

        public Node(){}

        public Node(vector2f pos)
        {
            m_pos = pos;
        }

        public Node(int ID){ m_id = ID; }

        /* ~~~ ID must be unique !!! ~~~ */
        public Node( int ID, vector2f pos ){
            m_id = ID;
            m_pos = pos;
        }

        public int GetID()
        {
            return m_id;
        }

	public int getNumLinks( )
	{
            return m_links.size();
	}
	
	public Link getLink( int index )
	{
            return m_links.get(index);
	}

        // note: does not bind the node to the link!
        public void addLink( Link newLink )
	{
            //if ( newLink.getNode(0).getPos().equals(m_pos) )
            m_links.add(newLink);
	}

        // returns true if this node is linked to the specified node
        public boolean isLinked(Node node){
            for(int i=0; i<m_links.size(); i++){
                Link p = m_links.get(i);
                if(p.getNode(0) == node || p.getNode(1) == node)
                    return true;
            }
            return false;
        }

        // removes the link but (does not!) update any of the links
        public void removeLink( Link erase )
	{
            m_links.remove(erase);
	}
	
	public vector2f getPos( )
	{
            return m_pos;
	}
	
	public void setPos( vector2f pos )
	{
            m_pos = pos;
	}
	
	
}
