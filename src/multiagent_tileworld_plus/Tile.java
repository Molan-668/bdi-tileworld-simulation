package multiagent_tileworld_plus;

public class Tile {
	private int id;
	private boolean claimed;
	
	public Tile(int id) {
		super();
		this.id = id;
		this.claimed = false;
	}

	public int getId() {
		return id;
	}
	
	public boolean isClaimed() {
		return this.claimed;
	}
	
	public void claim() {
		this.claimed = true;
	}
}
