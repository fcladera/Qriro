clear all;
close all;
%% Read log file
filename = 'Tests/simpleMov_Rotation.dat';
delimiter = {';',':'};
formatSpec = '%s%f%f%f%f%f%[^\n\r]';
fileID = fopen(filename,'r');
dataArray = textscan(fileID, formatSpec, 'Delimiter', delimiter, 'EmptyValue' ,NaN, 'ReturnOnError', false);
fclose(fileID);
sensorType = dataArray{:, 1};
id = dataArray{:, 2};
timeValue = dataArray{:, 3};
val1 = dataArray{:, 4};
val2 = dataArray{:, 5};
val3 = dataArray{:, 6};
clearvars filename delimiter formatSpec fileID dataArray ans;

%% Separate A and G values
min_size = min([length(sensorType);length(timeValue);length(val1);length(val2);length(val3)]);

gyroValues = zeros(min_size,4); 
accelValues = zeros(min_size,4);

gyroPointer=1;
accelPointer=1;

for i = 1:min_size
    if(strcmp(sensorType(i),'G'))
        gyroValues(gyroPointer,:) = [timeValue(i),val1(i),val2(i),val3(i)];
        gyroPointer = gyroPointer+1;
    elseif(strcmp(sensorType(i),'A'))
        accelValues(accelPointer,:) = [timeValue(i),val1(i),val2(i),val3(i)];
        accelPointer = accelPointer+1;
            
    end;
end;

% trim matrices
accelValues = accelValues(1:accelPointer-1,:);
gyroValues = gyroValues(1:gyroPointer-1,:);

clearvars accelPointer gyroPointer val1 val2 val3 sensorType i

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Gyro analysis
%% Simple plot

% figure;
% hold on;
% grid on;
% 
% plot(gyroValues(:,2),'r');
% plot(gyroValues(:,3),'b');
% plot(gyroValues(:,4),'g');


%% Simple mean value correction
  gyro_means = mean(gyroValues(1:150,2:4));
  gyroValues(:,2:4) =  gyroValues(:,2:4)-repmat(gyro_means,length(gyroValues),1);

%% Integrate over time
gyroIntegrated = zeros(length(gyroValues),3);

gyroIntegrated(1,:) = gyroValues(1,2:4)*gyroValues(1,1);
for i=2:length(gyroValues)
    gyroIntegrated(i,:) = gyroValues(i,1)*gyroValues(i,2:4)+gyroIntegrated(i-1,:);
end;


clearvars i


figure;
hold on;
grid on;
plot(gyroIntegrated(:,1),'r');
plot(gyroIntegrated(:,2),'g');
plot(gyroIntegrated(:,3),'b');

%% Plot base reference system x y z
figure;
hold on;
grid on;
axis([-1 1 -1 1 -1 1]);
quiver3(0,0,0,1,0,0,'r');
quiver3(0,0,0,0,1,0,'g');
quiver3(0,0,0,0,0,1,'b');
pause


%% Instantaneous matrix rotation calculation
instantaneous_rotation = eye(3);
for i=300:length(gyroValues)
    clf;
    axis([-1 1 -1 1 -1 1]);
    grid on;
    hold on;
    rotation = gyroValues(i,1)*gyroValues(i,2:4);
    
    Rx = [1 0 0;...
        0 cos(rotation(1)) -sin(rotation(1));...
        0 sin(rotation(1)) cos(rotation(1))];
    
    Ry = [cos(rotation(2)) 0 sin(rotation(2));...
          0 1 0;...
          -sin(rotation(2)) 0 cos(rotation(2))];
    Rz = [cos(rotation(3)) -sin(rotation(3)) 0;...
         sin(rotation(3)) cos(rotation(3)) 0;...
         0 0 1];
    Rot = Rx*Ry*Rz;
    instantaneous_rotation = instantaneous_rotation*Rot;
    if(mod(i,5)==0)
        quiver3(0,0,0,instantaneous_rotation(1,1),instantaneous_rotation(2,1),instantaneous_rotation(3,1),'r');
        quiver3(0,0,0,instantaneous_rotation(1,2),instantaneous_rotation(2,2),instantaneous_rotation(3,2),'g');
        quiver3(0,0,0,instantaneous_rotation(1,3),instantaneous_rotation(2,3),instantaneous_rotation(3,3),'b');
        pause
        i
    end
end

%% I
