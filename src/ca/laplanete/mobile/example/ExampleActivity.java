package ca.laplanete.mobile.example;

import android.app.Activity;
import android.os.Bundle;
import ca.laplanete.mobile.pageddragdropgrid.PagedDragDropGrid;

public class ExampleActivity extends Activity {

	
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.example);
		PagedDragDropGrid gridview = (PagedDragDropGrid) findViewById(R.id.gridview);		
		gridview.setAdapter(new ExamplePagedDragDropGridAdapter(this));
	}
}
