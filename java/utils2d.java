/**
 * 2D Math utilities
 */

public class utils2d
{

	// calculates an intersection point of two lines p2-p1 and p4-p3
	// returns: null - no intersection, else intersection point 
	// from: http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
	static vector2f intersect_lines(vector2f p1, vector2f p2, vector2f p3, vector2f p4)
	{
		float denom = (p4.y - p3.y)*(p2.x - p1.x) - (p4.x - p3.x)*(p2.y - p1.y);
		if(Math.abs(denom) <= vector2f.EPS) return null; // parallel lines
		float inv_denom = 1.0f/denom;
		
		float ua = ((p4.x - p3.x)*(p1.y - p3.y) - (p4.y - p3.y)*(p1.x - p3.x)) * inv_denom;
		if(ua < 0.0f || ua > 1.0f) return null; // not on line p2-p1
		float ub = ((p2.x - p1.x)*(p1.y - p3.y) - (p2.y - p1.y)*(p1.x - p3.x)) * inv_denom;
		if(ub < 0.0f || ub > 1.0f) return null; // not on line p4-p3
		
		vector2f p = new vector2f(p1);
		vector2f dir = p2.sub(p1);
		p.self_add(dir.mul(ua));
		return p;
	}
	
	// returns true if two spheres are intersecting
	static boolean intersect_spheres(vector2f p1, float radiusA, vector2f p2, float radiusB)
	{
		vector2f delta = p2.sub(p1);
		if(delta.length() <= radiusA+radiusB) return true;
		return false;
	}
	
	// calculates the closest point on line from another point near the line
	// returns: the point on line
	static vector2f point_on_line(vector2f p1, vector2f p2, vector2f p)
	{
		vector2f line = p2.sub(p1);
		float line_length = line.self_normalize();
		vector2f dir = p.sub(p1);
		float t = dir.dot(line);
		if(t < vector2f.EPS) return p1;
		else if(t > line_length) return p2;
		return p1.add(line.mul(t));
	}
}
