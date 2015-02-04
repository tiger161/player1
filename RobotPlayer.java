package player1;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer{
	
	static Direction facing;
	static Random rand;
	
	public static void run(RobotController rc){

	rand = new Random(rc.getID());
	facing = Direction.values()[(int)(rand.nextInt(8))];
	int role = 0;
	
	if(rc.getType()==RobotType.BEAVER){
		if(Clock.getRoundNum()>200){
			role = 1;
		}
	}
	
	while(true){
		try {
			if(rc.getType()==RobotType.HQ){
				makeDead(rc);
				if(Clock.getRoundNum()<500){
					spawnUnit(rc, Direction.NORTH, RobotType.BEAVER);
				}
			}
			else if(rc.getType()==RobotType.BEAVER && role == 0){
				makeDead(rc);
				if (rc.senseOre(rc.getLocation())>1&&rc.getLocation().distanceSquaredTo(rc.senseHQLocation())>4+(int)(Clock.getRoundNum()/50)){
					rc.mine();
				}
				randMove(rc);
			}
			
			else if (rc.getType()==RobotType.BEAVER && role == 1){
				makeDead(rc);
				if(Clock.getRoundNum()<300&&rc.getLocation().distanceSquaredTo(rc.senseHQLocation())>4+(int)(Clock.getRoundNum()/50)){
					buildUnit(rc, Direction.values()[(int)(rand.nextInt(8))], RobotType.MINERFACTORY);
				}
				else if (Clock.getRoundNum()>=300&&rc.getLocation().distanceSquaredTo(rc.senseHQLocation())>4+(int)(Clock.getRoundNum()/50)){
					buildUnit(rc, Direction.values()[(int)(rand.nextInt(8))], RobotType.BARRACKS);
				}
				
				if(rc.getLocation().distanceSquaredTo(rc.senseHQLocation())<20){
					randMove(rc);
				}				
				else if(rc.isCoreReady() && rc.canMove(rc.getLocation().directionTo(rc.senseHQLocation()))){
						rc.move((rc.getLocation().directionTo(rc.senseHQLocation())));
				}
			}
			
			else if(rc.getType()==RobotType.MINER){
				makeDead(rc);
				if (rc.senseOre(rc.getLocation())>1&&rc.getLocation().distanceSquaredTo(rc.senseHQLocation())>4){
					rc.mine();
				}
				randMove(rc);
			}
			
			else if(rc.getType()==RobotType.MINERFACTORY){
				if(Clock.getRoundNum()<700){
					spawnUnit(rc, rc.getLocation().directionTo(rc.senseEnemyHQLocation()), RobotType.MINER);
				}
			}
			
			else if(rc.getType()==RobotType.BARRACKS){
				spawnUnit(rc, Direction.NORTH, RobotType.SOLDIER);
			}
			
			else if(rc.getType() == RobotType.TOWER){
				makeDead(rc);
			}
			
			else if(rc.getType() == RobotType.SOLDIER){
				makeDead(rc);
				if (Clock.getRoundNum()<1800){
					if (rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation())>500){
						moveTowards(rc, rc.senseEnemyHQLocation());
					}
					else{
						randMove(rc);
					}
				}
				else{
					moveTowards(rc, rc.senseEnemyHQLocation());
				}
			}
			giveSuppies(rc);
		} catch (GameActionException e) {
			
			e.printStackTrace();
		}
		
		rc.yield();
	}
	
}
	
	private static void moveTowards(RobotController rc, MapLocation location) throws GameActionException {
		if(rc.isCoreReady()&&rc.canMove(rc.getLocation().directionTo(location))){
			rc.move(rc.getLocation().directionTo(location));
		}	
		else if(rc.isCoreReady()&&rc.canMove(rc.getLocation().directionTo(location).rotateLeft())){
			rc.move(rc.getLocation().directionTo(location).rotateLeft());
		}	
		else if(rc.isCoreReady()&&rc.canMove(rc.getLocation().directionTo(location).rotateRight())){
			rc.move(rc.getLocation().directionTo(location).rotateRight());
		}	
		else if(rc.isCoreReady()&&rc.canMove(rc.getLocation().directionTo(location).rotateLeft().rotateLeft())){
			rc.move(rc.getLocation().directionTo(location).rotateLeft().rotateLeft());
		}	
		else if(rc.isCoreReady()&&rc.canMove(rc.getLocation().directionTo(location).rotateRight().rotateRight())){
			rc.move(rc.getLocation().directionTo(location).rotateRight().rotateRight());
		}	
	}

	private static void giveSuppies(RobotController rc) throws GameActionException {
		double lowestSupplies = rc.getSupplyLevel();
		MapLocation suppliesHere = null;
		for(RobotInfo robot:rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam())){
			if(robot.supplyLevel<lowestSupplies){
				lowestSupplies = robot.supplyLevel;
				suppliesHere = robot.location;
			}
		}
		if(suppliesHere != null){
			rc.transferSupplies((int)(rc.getSupplyLevel()-lowestSupplies)/2, suppliesHere);
		}
	}

	private static void buildUnit(RobotController rc, Direction direction, RobotType type) throws GameActionException {
		if(rc.getTeamOre()>type.oreCost && rc.getLocation().distanceSquaredTo(rc.senseHQLocation())>5){
			if(rc.isCoreReady() && rc.canBuild(direction, type)){
				rc.build(direction, type);
			}
		}		
	}

	private static void spawnUnit(RobotController rc, Direction direction,RobotType type) throws GameActionException {
		if(rc.isCoreReady()&&rc.canSpawn(direction, type)){
			rc.spawn(direction, type);
		}
	}

	public static void randMove(RobotController rc) throws GameActionException{
		if(rand.nextDouble() < 0.1){
			if(rand.nextDouble() < 0.5){
				facing = facing.rotateLeft();
			}
			else{
				facing = facing.rotateRight();
			}
		}
		boolean safe = true;
		for(MapLocation tower:rc.senseEnemyTowerLocations()){
			if(rc.getLocation().add(facing).distanceSquaredTo(tower)<=RobotType.TOWER.attackRadiusSquared){
				safe = false;
			}
		}
		if(rc.senseTerrainTile(rc.getLocation().add(facing))!=TerrainTile.NORMAL||safe == false){
			facing = facing.rotateLeft();
		}
		if(rc.isCoreReady()&&rc.canMove(facing)){
			rc.move(facing);
		}
	}
	
	public static void makeDead(RobotController rc) throws GameActionException{
		RobotInfo[] robotsToKill = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,rc.getTeam().opponent());
		if(robotsToKill.length > 0){
			if(rc.isWeaponReady() == true && rc.canAttackLocation(robotsToKill[0].location)){
				rc.attackLocation(robotsToKill[0].location);
			}
		}
	}
	
}