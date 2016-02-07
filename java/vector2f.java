/**
 * Math classes
 */

public class vector2f
{
        static float EPS = 1e-6f;

	float x, y;
	
	public vector2f(){ x = 0; y = 0; }
	public vector2f(float a, float b){ x = a; y = b; }
	public vector2f(float v[]){ x = v[0]; y = v[1]; }
	public vector2f(float s){ x = s; y = s; }
	public vector2f(vector2f v){ x = v.x; y = v.y; }

        // copy self
        public vector2f copy(){ return new vector2f(x,y); }
	
	// aritmetines operacijos grazinancios rezultata vektoriu
	public vector2f mul(float s){ return new vector2f(x*s, y*s); }
	public vector2f div(float s){ return new vector2f(x/s, y/s); }
	public vector2f add(float s){ return new vector2f(x+s, y+s); }
	public vector2f sub(float s){ return new vector2f(x-s, y-s); }
	public vector2f inv(){ return new vector2f(-x, -y); }
	public vector2f mul(vector2f v){ return new vector2f(x*v.x, y*v.y); }
	public vector2f div(vector2f v){ return new vector2f(x/v.x, y/v.y); }
	public vector2f add(vector2f v){ return new vector2f(x+v.x, y+v.y); }
	public vector2f sub(vector2f v){ return new vector2f(x-v.x, y-v.y); }
	
	// aritmetines operacijos keiciancios pati vektoriu
	public void self_mul(float s){ x*=s; y*=s; }
	public void self_div(float s){ x/=s; y/=s; }
	public void self_add(float s){ x+=s; y+=s; }
	public void self_sub(float s){ x-=s; y-=s; }
	public void self_inv(){ x=-x; y=-y; }
	public void self_mul(vector2f v){ x*=v.x; y*=v.y; }
	public void self_div(vector2f v){ x/=v.x; y/=v.y; }
	public void self_add(vector2f v){ x+=v.x; y+=v.y; }
	public void self_sub(vector2f v){ x-=v.x; y-=v.y; }

        // comparing
        public boolean equals(vector2f v){ return (Math.abs(x-v.x)<=EPS && Math.abs(y-v.y)<=EPS); }
        public boolean equals(float a, float b){ return equals(new vector2f(a,b)); }
	
	// grazina ilgi ir ilgio kvadrata
	public float length(){ return (float)Math.sqrt(x*x + y*y); }
	public float square(){ return x*x + y*y; }
	
	// modulis
	public void abs(){ x = Math.abs(x); y = Math.abs(y); }
	
	// maksimalus/minimalus komponentas
	public float max(){ if(x>y) return x; return y; }
	public float min(){ if(x<y) return x; return y; }
	
	// normalizuoja vektoriu, grazina ilgi
	public float self_normalize(){ float len = length()+1e-6f; x /= len; y /= len; return len; } 
	// grazina normalizuota vektoriu 
	public vector2f normalized(){ vector2f v = new vector2f(this); v.self_normalize(); return v; } 
	
	// skaliarine sandauga
	public float dot(vector2f v){ return x*v.x + y*v.y; } 
	
	// vectorine sandauga su vektorium (0,0,1)
	// grazina siam vektoriui stamena vektoriu - nenormalizuota normale
	public vector2f cross(){ return new vector2f(y, -x); } 
}
