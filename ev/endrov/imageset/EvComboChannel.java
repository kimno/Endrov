package endrov.imageset;

import endrov.basicWindow.EvComboObjectOne;
import endrov.data.EvPath;

public class EvComboChannel extends EvComboObjectOne<EvChannel>
	{
	private static final long serialVersionUID = 1L;
	
	public EvComboChannel(boolean allowNoSelection, boolean allowCreation)
		{
		super(new EvChannel(), allowNoSelection, allowCreation);
		}
	
	public Imageset getImageset()
		{
		if(getSelectedObject()!=null)
			return (Imageset)getSelectObjectParent(); //TODO not always the case
		else
			return null;
		}
	
	public String getChannelName()
		{
		EvPath p=getSelectedPath();
		if(p!=null)
			return p.getLeafName();
		else
			return null;
		}
	
	
	//TODO: on updateList, re-get current item according to path! it might have been replaced
	
	}
