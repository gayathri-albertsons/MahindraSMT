package com.atpl.smt.userinterface;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atpl.smt.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class BarCodeScanner extends Fragment {

    public BarCodeScanner() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bar_code_scanner2, container, false);
    }

}
