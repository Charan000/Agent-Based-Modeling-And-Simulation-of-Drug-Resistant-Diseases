package repastcity3.agent;

import java.util.ArrayList;
import java.util.Collection;

import com.jcraft.jsch.Logger;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.query.space.gis.GeographyWithin;
import repastcity3.exceptions.ParameterNotFoundException;
import repastcity3.main.ContextManager;

public class DefaultAgentState {
	public enum State {
		U, LS, LR, AS, AR;
	}

	private State currentState;
	// Parameters for next_U. Decide if a healthy human goes to Latent state.
	private double prob_BS = 0.000137; // Higher this is , more chance of infection
	private double alpha = 0.5;
	private double prob_BR = prob_BS * alpha;
	// Parameters for next_L. Decide transition from Latent to Active States.
	private double prob_LA = 0.000031;
	// Parameters for next_A. Decide transition from Active to Latent after cure.
	private double prob_cured_A = 0.0095;
	private double prob_AR = 0.00002; // Maladministration at AS. (AS -> AR)
	private double resistanceFactor = 0.45;
	private double latentStart;
	private int latentFlag;
	private double activeSusceptibleStart;
	private double initialActiveResistantStart;
	private double activeResistantStart;
	private double LTimeAfterCureStart;
	private static int one_tick_sec = 60;
	private static double ticks_day = 24 * 60 * 60 / one_tick_sec;
	private static double realism_factor = 0.5;
	private static double ticks_month = 30 * ticks_day * realism_factor;
	private double LTime;
	private double LTimeAfterCure;
	private double ASTime;
	private double initialARTime;
	private double ARTime;

	public DefaultAgentState(State s) {
		
		try {
			prob_LA = Float.parseFloat(ContextManager.getParameter("prob_LA").toString());
		} catch (NumberFormatException | ParameterNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			prob_BS = Float.parseFloat(ContextManager.getParameter("prob_UL").toString());
		} catch (NumberFormatException | ParameterNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		this.latentFlag = 0;
		this.currentState = s;
		this.latentStart = 0;
		this.activeSusceptibleStart = 0;
		this.initialActiveResistantStart = 0;
		this.activeResistantStart = 0;
		this.LTimeAfterCureStart = 0;
		this.ASTime = 10 * ticks_month;
		this.initialARTime = 6 * ticks_month;
		this.ARTime = 25 * ticks_month;
		this.LTime = 5 * ticks_month;
		this.LTimeAfterCure = 6 * ticks_month;
	}

	public void nextState(DefaultAgent agent) {
		DefaultAgent.typeCount.put(agent.getCurrentState(), DefaultAgent.typeCount.get(agent.getCurrentState()) - 1);
		switch (this.currentState) {
		case U:
			this.currentState = next_U(agent);
			break;
		case LS:
			this.currentState = next_L(agent, false);
			break;
		case LR:
			this.currentState = next_L(agent, true);
			break;
		case AS:
			this.currentState = next_A(agent, false);
			break;
		case AR:
			this.currentState = next_A(agent, true);
			break;
		default:
			System.out.println("NO STATE MATCH");
			break;

		}
		DefaultAgent.typeCount.put(agent.getCurrentState(), DefaultAgent.typeCount.get(agent.getCurrentState()) + 1);

	}

	public State next_U(DefaultAgent agent) {
		GeographyWithin<DefaultAgent> aroundMe = new GeographyWithin<DefaultAgent>(ContextManager.getAgentGeography(),
				agent.getR(), agent);
		ArrayList<DefaultAgent> nearbyAgents = null;
		nearbyAgents = (ArrayList<DefaultAgent>) makeCollection(aroundMe.query());
		int n_BS = 0, n_BR = 0;
		for (DefaultAgent a : nearbyAgents) {
			if (a.getCurrentState() == State.AS)
				n_BS++;
			if (a.getCurrentState() == State.AR)
				n_BR++;
		}
		double prob_LS = 1.0 - Math.pow(1.0 - prob_BS, n_BS);
		double prob_LR = 1.0 - Math.pow(1.0 - prob_BR, n_BR);
		double prob_L = prob_LS + prob_LR - (prob_LS * prob_LR);
		if (Math.random() < prob_L) {
			double prob_L_LS = ((prob_LS) * (1 - prob_LR)) / prob_L;
			double prob_L_LR = 1 - prob_L_LS;
			latentStart = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			if (prob_L_LS > prob_L_LR) {
				return State.LS;
			} else {
				return State.LR;
			}
		}
		return State.U;
	}

	public State next_L(DefaultAgent agent, boolean isResistant) {
		if (latentFlag == 1) {
			// Assuming that patient will always be cured if he comes to Latent state after
			// getting cured from Active state.
			double currentTicks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			if (currentTicks - LTimeAfterCureStart < LTimeAfterCure) {
				return isResistant ? State.LR : State.LS;
			} else {
				latentFlag = 0;
				LTimeAfterCureStart = 0;
				return State.U;
			}
		} else {
			double currentTicks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			if (currentTicks - latentStart <= LTime) {
				return (isResistant) ? State.LR : State.LS;
			} else {
				double prob_A = prob_LA; // LR -> AR , LS _> AS
				double tChoice = Math.random();
				if (tChoice <= prob_A && (isResistant)) {
					initialActiveResistantStart = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
					latentStart = 0;
					return State.AR;
				}
				if (tChoice <= prob_A && (!isResistant)) {
					activeSusceptibleStart = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
					latentStart = 0;
					return State.AS;
				}
				return (isResistant) ? State.LR : State.LS;
			}
		}
	}

	public State next_A(DefaultAgent agent, boolean isResistant) {
		double prob_cured = prob_cured_A;
		if (isResistant) {
			prob_cured *= resistanceFactor;
			double currentTicks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			if (activeResistantStart > 0) {
				if (currentTicks - activeResistantStart >= ARTime) {
					if (Math.random() < prob_cured) {
						latentFlag = 1;
						activeResistantStart = 0;
						return State.LR;
					} else
						return State.AR;
				}
				return State.AR;
			} else {
				if (currentTicks - initialActiveResistantStart >= initialARTime) {
					if (Math.random() < prob_cured) {
						latentFlag = 1;
						initialActiveResistantStart = 0;
						LTimeAfterCureStart = currentTicks;
						return State.LR;
					} else {
						activeResistantStart = currentTicks;
						initialActiveResistantStart = 0;
						return State.AR;
					}
				}
				return State.AR;
			}
		} else {
			double currentTicks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			if (currentTicks - activeSusceptibleStart >= ASTime) {
				if (Math.random() < prob_cured) {
					LTimeAfterCureStart = currentTicks;
					activeSusceptibleStart = 0;
					return State.LS;
				} else {
					return State.AS;
				}
			} else {
				if (Math.random() <= prob_AR) {
					initialActiveResistantStart = currentTicks;
					activeSusceptibleStart = 0;
					return State.AR;
				} else {
					return State.AS;
				}
			}
		}
	}

	public double getActiveSusceptibleStart() {
		return activeSusceptibleStart;
	}

	public void setActiveSusceptibleStart(double x) {
		this.activeSusceptibleStart = x;
	}

	public double getActiveResistantStart() {
		return activeResistantStart;
	}

	public void setActiveResistantStart(double x) {
		this.activeResistantStart = x;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setState(State s) {
		this.currentState = s;
	}

	public static <E> Collection<E> makeCollection(Iterable<E> iter) {
		Collection<E> list = new ArrayList<E>();
		for (E item : iter) {
			list.add(item);
		}
		return list;
	}

}
