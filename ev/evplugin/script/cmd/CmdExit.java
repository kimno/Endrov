package evplugin.script.cmd;
import java.util.*;
import evplugin.script.*;
import javax.swing.*;
import evplugin.ev.*;

public class CmdExit extends Command
	{
	public int numArg()	{return 0;}
	public Exp exec(Vector<Exp> arg) throws Exception
		{
		if(EV.confirmQuit)
      {
      int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to quit?", "Quit?", JOptionPane.YES_NO_OPTION);
      if (option == JOptionPane.YES_OPTION)
      	EV.quit();
      }
		else
    	EV.quit();
		return null;
		}
	}