package multiagent_tileworld_plus;

public class Hole {
	private int id;
	private int score;
	private boolean claimed;

	public Hole(int id, int score) {
		super();
		this.id = id;
		this.score = score;
		this.claimed = false;
	}

	public int getId() {
		return id;
	}

	public int getScore() {
		return score;
	}
	
	public boolean isClaimed() {
		return this.claimed;
	}
	
	public void claim() {
		this.claimed = true;
	}
	
	public void release() {
		this.claimed = false;
	}
}
