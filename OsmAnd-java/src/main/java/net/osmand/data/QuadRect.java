package net.osmand.data;

public class QuadRect {
	public double left;
	public double right;
	public double top;
	public double bottom;

	// left & right / top & bottom could be flipped (so it's useful for latlon bbox) 
	public QuadRect(double left, double top, double right, double bottom) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}

	public QuadRect(QuadRect a) {
		this(a.left, a.top, a.right, a.bottom);
	}

	public QuadRect() {
	}


	public boolean invertedY() {
		// for latitude bbox
		return top > bottom;
	}
	
	public void expand(double left, double top, double right, double bottom) {
		if (hasInitialState()) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		} else {
			this.left = left < right ? Math.min(left, this.left) : Math.max(left, this.left);
			this.right = left < right ? Math.max(right, this.right) : Math.min(right, this.right);
			this.top = top < bottom ? Math.min(top, this.top) : Math.max(top, this.top);
			this.bottom = top < bottom ? Math.max(bottom, this.bottom) : Math.min(bottom, this.bottom);
		}
	}
	
	public double width() {
		return Math.abs(right - left);
	}

	public double height() {
		return Math.abs(bottom - top);
	}
	

	public boolean contains(double left, double top, double right, double bottom) {
		return Math.min(this.left, this.right) <= Math.min(left, right)
				&& Math.max(this.left, this.right) >= Math.max(left, right)
				&& Math.min(this.top, this.bottom) <= Math.min(top, bottom)
				&& Math.max(this.top, this.bottom) >= Math.max(top, bottom);
	}

	public boolean contains(QuadRect box) {
		return contains(box.left, box.top, box.right, box.bottom);
	}

	public static boolean intersects(QuadRect a, QuadRect b) {
		return Math.min(a.left, a.right) <= Math.max(b.left, b.right)
				&& Math.max(a.left, a.right) >= Math.min(b.left, b.right)
				&& Math.min(a.bottom, a.top) <= Math.max(b.bottom, b.top)
				&& Math.max(a.bottom, a.top) >= Math.min(b.bottom, b.top);
	}
	
	 public static boolean trivialOverlap(QuadRect a, QuadRect b) {
		 return intersects(a, b);
	 }

	public double centerX() {
		return (left + right) / 2;
	}

	public double centerY() {
		return (top + bottom) / 2;
	}

	public void offset(double dx, double dy) {
		left += dx;
		top += dy;
		right += dx;
		bottom += dy;
	}

	public void inset(double dx, double dy) {
		left += dx;
		top += dy;
		right -= dx;
		bottom -= dy;
	}

	public boolean hasInitialState() {
		return left == 0 && right == 0 && top == 0 && bottom == 0;
	}
	
	@Override
	public String toString() {
		return "[" + (float) left + "," + (float) top + " - " + (float) right + "," + (float) bottom + "]";
	}

}