/**
 * @(#) Link.java
 */
import java.awt.*;

public class Link
{
    private Node m_nodeA = null;
    private Node m_nodeB = null;

    public Link(){}

    public Link(Node a, Node b)
    {
        m_nodeA = a;
        m_nodeB = b;
    }

    // returns the node by index: 0 - A, otherwise - B
    public Node getNode( int index ) 
    {
        return (index == 0)?m_nodeA:m_nodeB;
    }

    // change the nodes of this link
    // note: the nodes must be updated(link removed) outside from this function
    public void setNodes( Node a, Node b )
    {
        m_nodeA = a;
        m_nodeB = b;
    }

    // repaint the link
    public void draw(Graphics2D painter)
    {
        vector2f p0 = m_nodeA.getPos();
        vector2f p1 = m_nodeB.getPos();
        painter.setStroke(new BasicStroke(0.25f));
        painter.drawLine((int)p0.x, (int)p0.y, (int)p1.x, (int)p1.y);
        painter.setStroke(new BasicStroke());
    }

}
