package ca.laplanete.mobile.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.laplanete.mobile.pageddragdropgrid.PagedDragDropGridAdapter;



public class ExamplePagedDragDropGridAdapter implements PagedDragDropGridAdapter {

	private Context context;
	
	List<Page> pages = new ArrayList<Page>();
	
	public ExamplePagedDragDropGridAdapter(Context context) {
		super();
		this.context = context;
		
		Page page1 = new Page();
		List<Item> items = new ArrayList<Item>();
		items.add(new Item(1, "Item 1", R.drawable.ic_launcher));
		items.add(new Item(2, "Item 2", R.drawable.ic_launcher));
		items.add(new Item(3, "Item 3", R.drawable.ic_launcher));
		page1.setItems(items);
		pages.add(page1);
		
		Page page2 = new Page();
		items = new ArrayList<Item>();
		items.add(new Item(4, "Item 4", R.drawable.ic_launcher));
		items.add(new Item(5, "Item 5", R.drawable.ic_launcher));
		items.add(new Item(6, "Item 6", R.drawable.ic_launcher));
		items.add(new Item(7, "Item 7", R.drawable.ic_launcher));
		items.add(new Item(8, "Item 8", R.drawable.ic_launcher));
		items.add(new Item(9, "Item 9", R.drawable.ic_launcher));
		page2.setItems(items);
		pages.add(page2);
		
		Page page3 = new Page();
		items = new ArrayList<Item>();
		items.add(new Item(10, "Item 10", R.drawable.ic_launcher));
		items.add(new Item(11, "Item 11", R.drawable.ic_launcher));
		items.add(new Item(12, "Item 12",R.drawable.ic_launcher));
		items.add(new Item(13, "Item 13",R.drawable.ic_launcher));
		items.add(new Item(14, "Item 14",R.drawable.ic_launcher));
		items.add(new Item(15, "Item 15",R.drawable.ic_launcher));
		items.add(new Item(16, "Item 16",R.drawable.ic_launcher));
		items.add(new Item(17, "Item 17",R.drawable.ic_launcher));
		items.add(new Item(18, "Item 18",R.drawable.ic_launcher));
		items.add(new Item(19, "Item 19",R.drawable.ic_launcher));
		items.add(new Item(20, "Item 20",R.drawable.ic_launcher));
		items.add(new Item(21, "Item 21",R.drawable.ic_launcher));
		items.add(new Item(22, "Item 22",R.drawable.ic_launcher));
		items.add(new Item(23, "Item 23",R.drawable.ic_launcher));
		items.add(new Item(24, "Item 24",R.drawable.ic_launcher));
		page3.setItems(items);
		pages.add(page3);
	}

	@Override
	public int pageCount() {
		return pages.size();
	}

	private List<Item> itemsInPage(int page) {
		if (pages.size() > page) {
			return pages.get(page).getItems();
		}	
		return Collections.emptyList();
	}

	@Override
	public View view(int page, int index) {
		
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		ImageView icon = new ImageView(context);
		Item item = getItem(page, index);
		icon.setImageResource(item.getDrawable());
		icon.setPadding(15, 15, 15, 15);
		
		layout.addView(icon);
		
		TextView label = new TextView(context);
		label.setText(item.getName());		
		label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
	
		label.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
		
		
		layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			
		layout.addView(label);
		return layout;
	}

	private Item getItem(int page, int index) {
		List<Item> items = itemsInPage(page);
		return items.get(index);
	}

	@Override
	public int rowCount() {
		return -1;
	}

	@Override
	public int columnCount() {
		return -1;
	}

	@Override
	public int itemCountInPage(int page) {
		return itemsInPage(page).size();
	}

	public void printLayout() {
		int i=0;
		for (Page page : pages) {
			Log.d("Page", Integer.toString(i++));
			
			for (Item item : page.getItems()) {
				Log.d("Item", Long.toString(item.getId()));
			}
		}
	}


	private Page getPage(int pageIndex) {
		return pages.get(pageIndex);
	}

	@Override
	public void swapItems(int pageIndex, int itemIndexA, int itemIndexB) {
		getPage(pageIndex).swapItems(itemIndexA, itemIndexB);
	}

	@Override
	public void moveItemToPageOnLeft(int pageIndex, int itemIndex) {
		int leftPageIndex = pageIndex-1;
		if (leftPageIndex >= 0) {
			Page startpage = getPage(pageIndex);
			Page landingPage = getPage(leftPageIndex);
			
			Item item = startpage.removeItem(itemIndex);
			landingPage.addItem(item);	
		}	
	}

	@Override
	public void moveItemToPageOnRight(int pageIndex, int itemIndex) {
		int rightPageIndex = pageIndex+1;
		if (rightPageIndex < pageCount()) {
			Page startpage = getPage(pageIndex);
			Page landingPage = getPage(rightPageIndex);
			
			Item item = startpage.removeItem(itemIndex);
			landingPage.addItem(item);			
		}	
	}
}
