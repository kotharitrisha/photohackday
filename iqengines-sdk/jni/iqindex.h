//
//  IQIndex.h
//  
//
//  Created by Pierre Garrigues on 6/9/11.
//  Copyright 2011 __MyCompanyName__. All rights reserved.
//

#include <vector>
#include <iostream>
#include <istream>

#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace cv;
using namespace std;

class IQIndexImp;

class IQIndex {
    
public:
    
    IQIndex();
    ~IQIndex();
    
    string data_dir;
    vector<string> obj_ids;
    map<string, vector<string> > obj_id2img_ids;
    map<string, string> obj_id2name;
    map<string, string> obj_id2meta;
    int num_images;
    
    int load(string, string);
    int train();
    int match(Mat& img);
    int compute(const Mat& img, const char*, const char*);
    
private:
    
    IQIndexImp *pimpl_;

};


