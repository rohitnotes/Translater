package com.dolbik.pavel.translater.fragments.note;


import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.dolbik.pavel.translater.R;
import com.dolbik.pavel.translater.activity.MainActivity;
import com.dolbik.pavel.translater.adapters.ViewPagerAdapter;
import com.dolbik.pavel.translater.fragments.favorite.FavoriteFragment;
import com.dolbik.pavel.translater.fragments.history.HistoryFragment;

public class NoteFragment extends MvpAppCompatFragment
        implements NoteView {


    @InjectPresenter  NotePresenter presenter;

    private ViewPager viewPager;
    private MenuItem  remove;
    private long      lastClickRemove = 0L;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Переопределяем menu. Override menu.
        setHasOptionsMenu(true);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
        int blackColor = ContextCompat.getColor(getContext(), R.color.black);
        int greyColor  = ContextCompat.getColor(getContext(), R.color.gray);
        tabs.setTabTextColors(greyColor, blackColor);
        tabs.setupWithViewPager(viewPager);
        setupViewPager();
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((MainActivity)getActivity()).setSupportActionBar(toolbar);
    }


    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
        Resources resources = getResources();
        adapter.addFragment(new HistoryFragment(),  resources.getString(R.string.hf_title));
        adapter.addFragment(new FavoriteFragment(), resources.getString(R.string.ff_title));
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) { presenter.setCurrentFragmentPosition(position); }
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        });
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_remove, menu);
        remove = menu.findItem(R.id.menu_remove);
        remove.setOnMenuItemClickListener(item -> {
            if (SystemClock.elapsedRealtime() - lastClickRemove < 1000) { return true; }
            lastClickRemove = SystemClock.elapsedRealtime();
            presenter.removeItems(viewPager.getCurrentItem());
            return true;
        });
        presenter.setCurrentFragmentPosition(viewPager.getCurrentItem());
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void showHideRemoveItem(boolean flag) {
        if (remove != null) {
            remove.setVisible(flag);
        }
    }
}
