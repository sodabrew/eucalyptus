package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface StartView extends IsWidget {
  void setPresenter(Presenter listener);
  
  public interface Presenter {
  }
  
}
