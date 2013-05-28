/**
 * Copyright 2012 
 * 
 * Nicolas Desjardins  
 * https://github.com/mrKlar
 * 
 * Facilite solutions
 * http://www.facilitesolutions.com/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ca.laplanete.mobile.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import ca.laplanete.mobile.pageddragdropgrid.OnPageChangedListener;
import ca.laplanete.mobile.pageddragdropgrid.PagedDragDropGrid;

public class ExampleActivity extends Activity implements OnClickListener {
    
    private String CURRENT_PAGE_KEY = "CURRENT_PAGE_KEY";
    
    private PagedDragDropGrid gridview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.example);
		gridview = (PagedDragDropGrid) findViewById(R.id.gridview);	
		
		ExamplePagedDragDropGridAdapter adapter = new ExamplePagedDragDropGridAdapter(this, gridview);
		
        gridview.setAdapter(adapter);
		gridview.setClickListener(this);

		gridview.setBackgroundColor(Color.LTGRAY);	
		
		
		gridview.setOnPageChangedListener(new OnPageChangedListener() {            
            @Override
            public void onPageChanged(PagedDragDropGrid sender, int newPageNumber) {
                Toast.makeText(ExampleActivity.this, "Page changed to page " + newPageNumber, Toast.LENGTH_SHORT).show();                
            }
        });
	}	
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
      int savedPage = savedInstanceState.getInt(CURRENT_PAGE_KEY);
      gridview.restoreCurrentPage(savedPage);
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {

	    outState.putInt(CURRENT_PAGE_KEY, gridview.currentPage());
	    super.onSaveInstanceState(outState);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Reset").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                gridview.setAdapter(new ExamplePagedDragDropGridAdapter(ExampleActivity.this, gridview));
                gridview.notifyDataSetChanged();
                
                return true;
            }
        });

        return true;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Clicked View", Toast.LENGTH_SHORT).show();
    }
}
