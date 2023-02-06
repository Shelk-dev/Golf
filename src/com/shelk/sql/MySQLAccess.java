package com.shelk.sql;

import com.shelk.Golf;
import com.shelk.GolfArea;
import com.shelk.utils.Utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MySQLAccess {
	Connection connection;

	FileConfiguration config;
	public final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public MySQLAccess(final FileConfiguration config) {
		this.config = config;
		new BukkitRunnable() {
			public void run() {
				try {
					if (MySQLAccess.this.connection != null && !MySQLAccess.this.connection.isClosed())
						MySQLAccess.this.connection.createStatement().execute("SELECT 1");
				} catch (SQLException e) {
					MySQLAccess.this.connection = MySQLAccess.this.getNewConnection(config);
				}
			}
		}.runTaskTimerAsynchronously(Golf.pl, 1200L, 1200L);
	}

	public Connection getNewConnection(FileConfiguration config) {
		String host = config.getString("mysql.host");
		String port = config.getString("mysql.port");
		String database = config.getString("mysql.database");
		String user = config.getString("mysql.user");
		String password = config.getString("mysql.password");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=latin1";
			Connection connection = DriverManager.getConnection(url, user, password);
			return connection;
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void close() throws SQLException {
		if (this.connection != null)
			this.connection.close();
	}

	public boolean execute(String sql) throws SQLException {
		boolean success = this.connection.createStatement().execute(sql);
		return success;
	}

	public boolean checkConnection() throws SQLException {
		if (this.connection == null || this.connection.isClosed()) {
			this.connection = getNewConnection(this.config);
			if (this.connection == null || this.connection.isClosed())
				return false;
			execute("CREATE TABLE IF NOT EXISTS golfAreas (id VARCHAR(36) PRIMARY KEY, balls TEXT, cauldrons TEXT)");
			
			execute("CREATE TABLE IF NOT EXISTS scores (uuid VARCHAR(36) PRIMARY KEY, scores TEXT, hide BOOLEAN, dates TEXT)");
		}
		return true;
	}

	public boolean init() {
		try {
			return checkConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public ArrayList<GolfArea> getAreas() {
		ArrayList<GolfArea> areas = new ArrayList<>();
		try {
			String sql = "SELECT id FROM golfAreas";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			ResultSet set = stmt.executeQuery();
			while (set.next())
				areas.add(getArea(set.getString("id")));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return areas;
	}

	public GolfArea getArea(String id) {
		try {
			String sql = "SELECT * FROM golfAreas WHERE id=?";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, id);

			ResultSet set = stmt.executeQuery();
			if (set.next()) {

				HashMap<String, Location> balls = Utils.convertString(set.getString("balls"));
				if (balls == null) balls = new HashMap<>();
				HashMap<String, Location> cauldrons = Utils.convertString(set.getString("cauldrons"));
				if (cauldrons == null) cauldrons = new HashMap<>();
				return new GolfArea(id, balls, cauldrons);

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean getHide(String uuid) {
		try {
			String sql = "SELECT * FROM scores WHERE uuid=?";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, uuid);

			ResultSet set = stmt.executeQuery();
			if (set.next()) {
				return set.getBoolean("hide");

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean setHide(String uuid, boolean hide) {
		try {

			if (Golf.mySql.getScores(uuid) != null && Golf.mySql.getScores(uuid).equals("novalue")) {
				String sql = "INSERT INTO scores (uuid) VALUES (?)";
				PreparedStatement stmt = this.connection.prepareStatement(sql);
				stmt.setString(1, uuid);
				stmt.executeUpdate();
			}

			String sql = "UPDATE scores SET hide=? WHERE uuid=?;";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setBoolean(1, hide);
			stmt.setString(2, uuid);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getScores(String uuid) {
		try {
			String sql = "SELECT * FROM scores WHERE uuid=?";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, uuid);

			ResultSet set = stmt.executeQuery();
			if (set.next()) {
				return set.getString("scores");
			} else
				return "novalue";

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int getScoresInOne(String uuid) {
		
		String[] scores = Golf.mySql.getScores(uuid).split(",");
		int count = 0;
		for (String score : scores) {
			if (score.split("=").length > 1 && score.split("=")[1].equals("1")) {
				count++;
			}
		}
		return count;
	}
	
	public String getDates(String uuid) {
		try {
			String sql = "SELECT * FROM scores WHERE uuid=?";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, uuid);

			ResultSet set = stmt.executeQuery();
			if (set.next()) {
				return set.getString("dates");
			} else
				return "novalue";

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getDate(Player p, String id) {
		if (Golf.mySql.getDates(p.getUniqueId().toString()) == null) {
			return null;
		}

		String[] scores = Golf.mySql.getDates(p.getUniqueId().toString()).split(",");
		for (String score : scores) {
			if (score.split("=")[0].equalsIgnoreCase(id)) {
				return score.split("=")[1];
			}
		}
		return null;
	}

	public int getScore(Player p, String id) {
		if (Golf.mySql.getScores(p.getUniqueId().toString()) == null) {
			return 0;
		}

		String[] scores = Golf.mySql.getScores(p.getUniqueId().toString()).split(",");
		for (String score : scores) {
			if (score.split("=")[0].equalsIgnoreCase(id)) {
				return Integer.parseInt(score.split("=")[1]);
			}
		}
		return 0;
	}

	public void addGolfArea(GolfArea area) {
		try {
			String sql = "INSERT INTO golfAreas (id) VALUES (?)";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, area.getAreaId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addScores(Player p, String scores) {
		try {
			String sql = "INSERT INTO scores (uuid,scores) VALUES (?,?)";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, p.getUniqueId().toString());
			stmt.setString(2, scores);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateArea(GolfArea area) {
		try {
			String sql = "UPDATE golfAreas SET balls=?, cauldrons=? WHERE id=?;";
			PreparedStatement stmt = this.connection.prepareStatement(sql);

			stmt.setString(1, Utils.convertMap(area.getBalls()));
			stmt.setString(2, Utils.convertMap(area.getCauldrons()));
			stmt.setString(3, area.getAreaId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateScore(Player p, String scoreToAdd) {
		try {
			String sql = "UPDATE scores SET scores=? WHERE uuid=?;";
			PreparedStatement stmt = this.connection.prepareStatement(sql);

			String scores = getScores(p.getUniqueId().toString());
			ArrayList<String> scoresNew = new ArrayList<>();
			if (scores != null) {
				String[] scoresArray = scores.split(",");

				for (String score : scoresArray) {
					if (!score.split("=")[0].equals(scoreToAdd.split("=")[0]))
						scoresNew.add(score);
				}
			}
			scoresNew.add(scoreToAdd);

			stmt.setString(1, StringUtils.join(scoresNew, ","));
			stmt.setString(2, p.getUniqueId().toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateDate(Player p, String dateToAdd) {
		try {
			String sql = "UPDATE scores SET dates=? WHERE uuid=?;";
			PreparedStatement stmt = this.connection.prepareStatement(sql);

			String dates = getDates(p.getUniqueId().toString());
			ArrayList<String> datesNew = new ArrayList<>();
			if (dates != null) {
				String[] datesArray = dates.split(",");

				for (String date : datesArray) {
					if (!date.split("=")[0].equals(dateToAdd.split("=")[0]))
						datesNew.add(date);
				}
			}
			datesNew.add(dateToAdd);

			stmt.setString(1, StringUtils.join(datesNew, ","));
			stmt.setString(2, p.getUniqueId().toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void deleteArea(String id) {
		try {
			String sql = "DELETE FROM golfAreas WHERE id=?;";
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
