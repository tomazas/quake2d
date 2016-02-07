/**
 * @(#) Graph.java
 */
import java.util.*;
import java.awt.*;

public class Graph
{
    final int NODE_RADIUS = 5;
    final int NODE_DIST = 15; // pixels

    private ArrayList<Link>  m_links = null;
    private ArrayList<Node>  m_nodes = null;
    private World            m_world = null;
    private Node[][]         m_nodemap = null;
    private int[]            m_mapbounds = null;
    private boolean          m_built = false;

    public Graph(World world){
        m_world = world;
    }

    // generates the graph from world data
    public void build( float x, float y )
    {
        // calc map bounds
        float b[] = this.computeBounds();
        m_mapbounds = new int[4];
        m_mapbounds[0] = (int)(b[0]-1); // -+1 to increase the bounds
        m_mapbounds[1] = (int)(b[1]-1);
        m_mapbounds[2] = (int)(b[2]+1);
        m_mapbounds[3] = (int)(b[3]+1);

        // create node map
        int cols = (m_mapbounds[2] - m_mapbounds[0])/NODE_DIST;
        int rows = (m_mapbounds[3] - m_mapbounds[1])/NODE_DIST;
        m_nodemap = new Node[rows][cols];

        // generate graph nodes
        m_nodes = new ArrayList<Node>();
        m_links = new ArrayList<Link>();
        generateNodesRecursive(new vector2f(x,y));
        linkNodes();

        System.out.println("--- Graph characteristics ---");
        System.out.println("Nodes created: "+m_nodes.size());
        System.out.println("Links created: "+m_links.size());

        m_built = true;
    }

    public void destroy( )
    {

    }

    public void draw( Graphics2D painter )
    {
        if(!m_built) return;

        // draw links
        painter.setColor(Color.BLUE);

        int num = m_links.size();
        for(int i = 0; i<num; i++ )
            m_links.get(i).draw(painter);

        // draw nodes
        painter.setColor(Color.GRAY);

        int size = m_nodes.size();
        for(int i = 0; i<size; i++ )  {
            vector2f vector = m_nodes.get(i).getPos();
            painter.fillOval((int)vector.x - NODE_RADIUS/2,
                        (int)vector.y - NODE_RADIUS/2,
                        NODE_RADIUS, NODE_RADIUS);
         }

        painter.setColor(Color.BLACK);
    }

    public int getNumLinks( )
    {
        return m_links.size();
    }

    public Link getLink( int index )
    {
        return m_links.get(index);
    }

    public int getNumNodes( )
    {
        return m_nodes.size();
    }

    public Node getNode( int index )
    {
        return m_nodes.get(index);
    }

    public Path findPath( vector2f from, vector2f to/*, Path path */)
    {
      final boolean use_dijkstra_v2 = true;

        Node startNode = this.closestToPoint(from);
        if(startNode == null) return null;

        Node endNode = this.closestToPoint(to);
        if(endNode == null) return null;

        // find path using Dijkstra
        int num = m_nodes.size();
        float dist[] = new float[num];  // nodes' accumulated distance
        Node parents[] = new Node[num]; // nodes' parents (where we came from)

        // zero everything (Java does this, but just to be sure)
        for(int i=0; i<num; i++) dist[i] = 0.0f;
        for(int i=0; i<num; i++) parents[i] = null;

        // queue for processing nodes
        LinkedList<Node> q = new LinkedList<Node>();

        // start the show ;)
        parents[startNode.GetID()] = startNode;
        q.push(startNode);


        while( !q.isEmpty() )
        {
            Node node = q.pop();

            int n = node.getNumLinks();
            for(int i=0; i<n; i++){
                Link p = node.getLink(i);
                Node next = null;

                if(p.getNode(0) == node)
                    next = p.getNode(1);
                else
                    next = p.getNode(0);

                float weight = next.getPos().sub(node.getPos()).length();
                float new_weight = dist[node.GetID()] + weight;

                int id = next.GetID();
                if(parents[id] == null){ // first time visited
                    parents[id] = node;
                    dist[id] = new_weight;
                    q.add(next); // check this node later
                }else{ // visited one more time
                    // check weights
                    if(dist[id] > new_weight){
                        parents[id] = node;
                        dist[id] = new_weight;
                    }
                }
            }

            // now check next node
        }

        // there's no path to that node
        if(parents[endNode.GetID()] == null){
            System.out.println("Pathfinder. Unreachable node: "+endNode+" from "+startNode);
            return null;
        }

        LinkedList<Node> revpath = new LinkedList<Node>();
        Node node = endNode;
        while(node != startNode){
            revpath.addFirst(node);
            node = parents[node.GetID()];
        }
        revpath.addFirst(startNode);

        Path path = new Path(from, to);
        for(int i=0; i<revpath.size(); i++)
            path.appendNode(revpath.get(i));

        return path;
    }

    // returns the closest node to a point
    public Node closestToPoint( vector2f pos )
    {
        Node pick = null;
        float prevLen = -1.0f;
        int num = m_nodes.size();

        for(int i=0; i<num; i++)
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

    // links the nodes based on neighbours from
    // the node map
    private void linkNodes(){

        // these contain offsets from starting cell to
        // neighbouring cells:  new_cell = current_cell + offset
        int offset[]  = {1,0, 0,1, -1,0, 0,-1, 1,1, -1,1, -1,-1, 1,-1};

        int rows = m_nodemap.length;
        int cols = m_nodemap[0].length;

        for(int y=0; y<rows; y++){
            for(int x=0; x<cols; x++){
                Node curr = m_nodemap[y][x]; // node assigned to current cell
                if(curr == null) continue; // not assigned
                
                for(int k=0; k<8; k++){
                    int new_x = x + offset[k*2];
                    int new_y = y + offset[k*2+1];

                    // check bounds
                    if(new_x < 0 || new_y < 0) continue;
                    if(new_x >= cols || new_y >= rows) continue;

                    Node node = m_nodemap[new_y][new_x];
                    if(node == null) continue; // no neighbour
                    if(node.isLinked(curr)) continue;
                    
                    Link newLink = new Link(curr, node);
                    node.addLink(newLink);
                    curr.addLink(newLink);
                    m_links.add(newLink);
                }
            }
        }
    }

    // computes a node's cell and places it in a node 2D array for quick
    // neighbour look-up
    private void addToNodeMap(Node node){
        int half = NODE_DIST/2; // make this conversion more precise
        int cell_x = (int)((node.getPos().x-m_mapbounds[0]-half)/NODE_DIST);
        int cell_y = (int)((node.getPos().y-m_mapbounds[1]-half)/NODE_DIST);
        if(m_nodemap[cell_y][cell_x] != null)
            System.out.println("Collision!");
        m_nodemap[cell_y][cell_x] = node;
    }

    // calculates map bounds by checking wall end points
    private float[] computeBounds(){
        int n_walls = m_world.getNumWalls();
        vector2f min = new vector2f(9999999,9999999);
        vector2f max = new vector2f(-9999999,-9999999);

        for( int i = 0; i< n_walls; i++){
            Wall wall = m_world.getWall(i);
            vector2f w_start = wall.getStart();
            vector2f w_end   = wall.getEnd();

            if(w_start.x >  max.x) max.x = w_start.x;
            if(w_start.y >  max.y) max.y = w_start.y;
            if(w_end.x >  max.x) max.x = w_end.x;
            if(w_end.y >  max.y) max.y = w_end.y;

            if(w_start.x <  min.x) min.x = w_start.x;
            if(w_start.y <  min.y) min.y = w_start.y;
            if(w_end.x <  min.x) min.x = w_end.x;
            if(w_end.y <  min.y) min.y = w_end.y;
        }

        float arr[] = {min.x, min.y, max.x, max.y};
        return arr;
    }

    // Launches four rays in diferent directions from 'pos' to find the nearest
    // point of intersection on the walls. Places a new node only if the
    // distance threshold is met.
    // Returns the minimum distance from the 'pos' point to the closest point
    // on any of the walls. Returns -1.0f if there's no intersection at all.
    private float closestHit(vector2f pos){
        // build ray "ends"
        vector2f ray_ends[] = {
            new vector2f( pos.x, pos.y-99999), // up
            new vector2f( pos.x+99999, pos.y), // right
            new vector2f( pos.x, pos.y+99999), // down
            new vector2f( pos.x-99999, pos.y)  // left
        };

        float minDist = -1.0f; // "not set"

        // iterate through all walls and intersect
        int n_walls = m_world.getNumWalls();
        for ( int i = 0; i< n_walls; i++ )  {
            Wall wall = m_world.getWall(i);
            vector2f w_start = wall.getStart();
            vector2f w_end   = wall.getEnd();

            for(int j=0; j<4; j++){
                vector2f hit = utils2d.intersect_lines(pos, ray_ends[j], w_start, w_end);
                if(hit == null) continue;
                vector2f dif = pos.sub(hit);
                float dist = dif.length();
                if(dist < minDist || minDist < 0.0f)
                    minDist = dist;
            }
        }

        return minDist;
    }

    // Generates nodes from a starting "seed/beacon" node by
    // the floodfill method. After this call, nodes need to be linked
    // to their neighbours by using the nodemap.
    private void generateNodesRecursive(vector2f pos) {

        float minDist = closestHit(pos);
        
        if(minDist > NODE_DIST && isNodePlaced(pos) == false)
        {
            // approve this node
            Node node = new Node( m_nodes.size(), new vector2f(pos.x, pos.y) );
            m_nodes.add( node );
            addToNodeMap(node);

            // now expand & do the checking later
            generateNodesRecursive(new vector2f(pos.x,pos.y+NODE_DIST)); // down
            generateNodesRecursive(new vector2f(pos.x+NODE_DIST,pos.y)); // right
            generateNodesRecursive(new vector2f(pos.x,pos.y-NODE_DIST)); // up
            generateNodesRecursive(new vector2f(pos.x-NODE_DIST,pos.y)); // left
        }
    }

    // checks if a node is already placed in position 'pos'.
    private boolean isNodePlaced(vector2f pos) {
        for ( int i = 0; i < m_nodes.size(); i++ )  {
            vector2f vector = m_nodes.get(i).getPos();
            if ( vector.equals( pos ) )  {
                return true;
            }
        }
        return false;
    }

//    private boolean notEmpty(Node[] allnodes, int node_size) {
//        boolean exists = false;
//        for ( int i = 0; i < node_size && !exists; i++ )  {
//            if ( allnodes[i] != null )
//                exists = true;
//        }
//        return exists;
//    }

    // finds a node with the minimal distance
    private int findMin( int[] dist, int num, int infinity, Node[] allnodes) {
        int mindist = infinity;
        int minindex = -1;
        for ( int i = 0; i < num; i++ )  {
            if ( dist[i] < mindist && allnodes[i] != null )  {
                mindist = dist[i];
                minindex = i;
            }
        }
        return minindex;
    }

    // finds a node's index inside the 'allnodes' array
    private int findTarget(vector2f to, Node[] allnodes, int num) {
        int target = -1;
        for ( int i = 0; i < num; i++ ) {
            if ( allnodes[i] != null )
                if ( allnodes[i].getPos().equals(to)  )
                    target = i;
            }
        return target;
    }
	
}
