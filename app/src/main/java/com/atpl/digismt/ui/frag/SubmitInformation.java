package com.atpl.digismt.ui.frag;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atpl.digismt.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubmitInformation extends Fragment {


    public SubmitInformation() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_submit_information, container, false);


        return view;

    }

}
